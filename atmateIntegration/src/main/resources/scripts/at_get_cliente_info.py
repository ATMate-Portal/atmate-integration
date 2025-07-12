import pickle
import requests
from bs4 import BeautifulSoup
import json # Importar para lidar com JSON aninhado
import os
import re
from urllib.parse import quote_plus # Importar para URL encoding
import sys # Para output UTF-8


# --- Regexes Comuns para Extração de Valores de JavaScript ---
# Apanha 'chave: 'valor'' OU 'chave: stringOrNull('valor')'
# O grupo 1 apanha o valor direto, o grupo 2 apanha o valor dentro de stringOrNull
GENERIC_VALUE_REGEX_DIRECT_FIRST = r"{key}:\s*(?:'([^']*)'|stringOrNull\('([^']*)'\))"
GENERIC_VALUE_REGEX_FUNC_FIRST = r"{key}:\s*(?:stringOrNull\('([^']*)'\)|'([^']*)')"
# Regex para token CSRF (com grupo nomeado 'token')
CSRF_TOKEN_REGEX = r"_csrf:\s*\{\s*parameterName:\s*'[^']*',?\s*token\s*:\s*'(?P<token>[^']+)',?.*?\}"
# Regex para JSON.parse()
JSON_PARSE_REGEX = r"{key}:\s*JSON\.parse\('(.+?)'\s*\|\|\s*null\)"


# --- Função Auxiliar de Extração de JS ---
def extract_js_data(html_soup, script_identifier, field_patterns):
    """
    Extrai dados de um bloco JavaScript dentro do HTML.
    html_soup: Objeto BeautifulSoup da página.
    script_identifier: String para procurar no script para identificar o bloco correto (ex: 'var model =').
    field_patterns: Dicionário {chave_a_extrair: (regex_pattern_1, regex_pattern_2, ...)}
                    As regexes devem ter um grupo de captura para o valor.
                    Pode incluir um padrão especial para JSON.parse.
    """
    script_content = None
    for script_tag in html_soup.find_all('script', type='text/javascript'):
        if script_tag.string and script_identifier in script_tag.string:
            script_content = script_tag.string
            break

    if not script_content:
        raise ValueError(f"Script contendo '{script_identifier}' não encontrado no HTML.")

    extracted_data = {}
    for key, patterns in field_patterns.items():
        value = None
        
        for pattern in patterns:
            if pattern is None: continue # Saltar se o padrão for None
            
            # --- CASOS ESPECIAIS ---
            if key == '_csrf_token_special': # Chave especial para o CSRF
                match = re.search(pattern, script_content, re.DOTALL)
                if match and match.group('token'):
                    value = match.group('token')
                    break # Encontrou, sai do loop de padrões
            elif key.endswith('_json_parse'): # Campo que contém um JSON string (ex: 'credential_json_parse')
                # A chave para a regex é sem o '_json_parse'
                regex_key = key.replace('_json_parse', '')
                formatted_pattern = pattern.format(key=re.escape(regex_key)) 
                match = re.search(formatted_pattern, script_content, re.DOTALL) # DOTALL para JSON multi-linha
                if match:
                    json_str = match.group(1)
                    if json_str: # Se a string JSON não for vazia
                        try:
                            value = json.loads(json_str) # Parseia a string JSON
                            break # Encontrou e parseou, sai do loop de padrões
                        except json.JSONDecodeError:
                            # Não conseguiu parsear como JSON, tenta o próximo padrão ou falha
                            pass
            # --- CASOS GENÉRICOS ---
            else:
                # Formatar a regex com a chave atual
                formatted_pattern = pattern.format(key=re.escape(key))
                match = re.search(formatted_pattern, script_content)
                if match:
                    # Captura o valor do primeiro grupo que não é None
                    value = match.group(1) if match.group(1) else (match.group(2) if len(match.groups()) >= 2 else None)
                    if value is not None:
                        break # Encontrou, sai do loop de padrões

        if value is not None:
            extracted_data[key] = value
        else:
            raise ValueError(f"Valor '{key}' não encontrado no script JavaScript identificado por '{script_identifier}'.")
            
    return extracted_data


# --- Início do Script Principal ---
try:
    getTypeFromAT = os.environ.get("getTypeFromAT")
    if not getTypeFromAT:
        getTypeFromAT = True # Convert to boolean later if used as boolean

    nif = os.environ.get("NIF")
    if not nif:
        nif = ""
    password = os.environ.get("PASSWORD")
    if not password:
        password = ""
    scriptPath = os.environ.get("SCRIPT_PATH")
    if not scriptPath:
        scriptPath = "src/main/resources/scripts/"

    session_file_name = f'session_{nif}.pkl'
    jsessionid_file_name = f'JSessionID_{nif}.pkl'

    session_file_path = os.path.join(scriptPath, session_file_name)
    jsessionid_file_path = os.path.join(scriptPath, jsessionid_file_name)


    # Carregar sessão autenticada
    with open(session_file_path, 'rb') as f:
        session = pickle.load(f)

    # Adicionar cookie "autentica_JSessionID" guardada previamente
    with open(jsessionid_file_path, 'rb') as f:
        cookie_value = pickle.load(f)
    session.cookies.set('autentica_JSessionID', cookie_value)

    # CHAMADA 1 - GET inicial para /integrada (opcional)
    # Não há extração de dados neste passo, URL e headers fixos.
    session.get(
        'https://sitfiscal.portaldasfinancas.gov.pt/integrada',
        headers={
            'User-Agent': 'Mozilla/5.0',
            'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard',
        },
        allow_redirects=False
    )

    # CHAMADA 2 - Obter formulário da SSO (AGORA COM HTML CONTENDO 'model')
    response_chamada2 = session.get(
        'https://www.acesso.gov.pt/v2/loginForm?partID=SFIC&path=/integrada',
        headers={'User-Agent': 'Mozilla/5.0'},
        allow_redirects=False
    )
    soup_chamada2 = BeautifulSoup(response_chamada2.text, 'html.parser')

    # Definição dos padrões para a extração do objeto 'model' da CHAMADA 2
    js_model_chamada2_patterns = {
        'partID': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        'path': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        '_csrf_token_special': (CSRF_TOKEN_REGEX,),
        'action': (GENERIC_VALUE_REGEX_FUNC_FIRST, GENERIC_VALUE_REGEX_DIRECT_FIRST),
        'credential_json_parse': (JSON_PARSE_REGEX,) 
    }

    # Extrair os dados do objeto 'model' do HTML da Chamada 2
    extracted_data_chamada2 = extract_js_data(soup_chamada2, 'var model =', js_model_chamada2_patterns)

    # Preparar os dados para o POST da CHAMADA 3
    data_post_chamada3 = {}
    
    # Adicionar os campos básicos do model
    data_post_chamada3['_csrf'] = extracted_data_chamada2.get('_csrf_token_special')
    data_post_chamada3['path'] = extracted_data_chamada2.get('path')
    data_post_chamada3['partID'] = extracted_data_chamada2.get('partID')

    # Se o credential contiver os dados para o POST, extraí-los e adicionar
    credential_data = extracted_data_chamada2.get('credential_json_parse', {})
    for key, value in credential_data.items():
        data_post_chamada3[key] = value

    # Adicionar NIF e PASSWORD diretamente das variáveis de ambiente / default
    # Estas variáveis (nif, password) estão definidas no início do script e são acessíveis aqui
    data_post_chamada3['username'] = nif
    data_post_chamada3['password'] = password
    data_post_chamada3['selectedAuthMethod'] = 'N' # ou extrair do model se for dinâmico

    # O URL para o POST da CHAMADA 3 vem do campo 'action' do model
    post_url_chamada3 = extracted_data_chamada2.get('action')
    if not post_url_chamada3:
        raise ValueError("URL de ação para POST da Chamada 3 não encontrado no script JavaScript.")

    # CHAMADA 3 - POST para autenticar acesso ao módulo integrada
    response_chamada3 = session.post(
        post_url_chamada3,
        headers={
            'User-Agent': 'Mozilla/5.0',
            'Content-Type': 'application/x-www-form-urlencoded',
            'Origin': 'https://www.acesso.gov.pt',
            'Referer': 'https://www.acesso.gov.pt/',
        },
        data=data_post_chamada3, # requests.post envia um dict e faz encoding
        allow_redirects=False
    )

    # CHAMADA 4 - Redirecionamento para /integrada
    redirect_url = response_chamada3.headers.get("Location")
    if not redirect_url:
        raise ValueError("URL de redirecionamento (Location header) não encontrado após POST da Chamada 3.")
    
    if redirect_url.startswith("/"):
        redirect_url = "https://sitfiscal.portaldasfinancas.gov.pt" + redirect_url 
    elif not redirect_url.startswith("http"):
        raise ValueError(f"URL de redirecionamento inválido: {redirect_url}")

    response_chamada4 = session.get(redirect_url, headers={'User-Agent': 'Mozilla/5.0'})

    # CHAMADA 5 - GET para presentation?httpRefererTransId=...
    http_referer_trans_id = 'e2383a35-00ce-44ea-8ff5-97c9918d6ce2' # Manter fixo se não soubermos a fonte

    response_chamada5 = session.get(
        'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation',
        params={'httpRefererTransId': http_referer_trans_id},
        headers={'User-Agent': 'Mozilla/5.0'}
    )

    soup_chamada5 = BeautifulSoup(response_chamada5.text, "html.parser")

    dados_cliente = {}

    if getTypeFromAT:
        atividade_exercida_encontrada = "Atividade Exercida" in soup_chamada5.get_text()
        dados_cliente["atividade_exercida_encontrada"] = atividade_exercida_encontrada

        if atividade_exercida_encontrada:
            atividade_link = soup_chamada5.find("a", title="Consultar Atividade Exercida")
            if atividade_link and atividade_link.get("href"):
                base_url = "https://sitfiscal.portaldasfinancas.gov.pt/integrada/"
                full_url = base_url + atividade_link["href"]

                headers_atividade = {
                    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
                    'Accept-Language': 'pt-PT,pt;q=0.8',
                    'Cache-Control': 'no-cache',
                    'Connection': 'keep-alive',
                    'Pragma': 'no-cache',
                    'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation', 
                    'Sec-Fetch-Dest': 'document',
                    'Sec-Fetch-Mode': 'navigate',
                    'Sec-Fetch-Site': 'same-origin',
                    'Sec-Fetch-User': '?1',
                    'Sec-GPC': '1',
                    'Upgrade-Insecure-Requests': '1',
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36',
                    'sec-ch-ua': '"Brave";v="135", "Not-A.Brand";v="8", "Chromium";v="135"',
                    'sec-ch-ua-mobile': '?0',
                    'sec-ch-ua-platform': '"Windows"',
                }

                response_atividade = session.get(
                    full_url,
                    cookies=session.cookies,
                    headers=headers_atividade,
                )

                soup_atividade = BeautifulSoup(response_atividade.text, "html.parser")

                cessacao_element = soup_atividade.find("dt", string="Data de Cessação")
                if cessacao_element:
                    cessacao_date = cessacao_element.find_next("dd").get_text(strip=True)
                    dados_cliente["data_cessacao"] = cessacao_date if cessacao_date != "-" else None
                else:
                    dados_cliente["data_cessacao"] = None

    # CHAMADA 6 - GET final com queryStringS e hmac
    response_chamada6 = session.get(
        'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation',
        params={
            'queryStringS': 'targetScreen=ecraIdentificacao&hmac=z1DA4exOnazVrAB2PGbKeRYtiOk=' 
        },
        headers={'User-Agent': 'Mozilla/5.0'}
    )

    soup_chamada6 = BeautifulSoup(response_chamada6.text, "html.parser")

    campos_desejados = {
        "NIF": "nif",
        "Nome": "nome",
        "Data de Nascimento": "data_nascimento",
        "Sexo": "sexo",
        "Distrito": "distrito",
        "Concelho": "concelho",
        "Freguesia": "freguesia",
        "País de Residência": "pais_residencia",
        "Nacionalidade": "nacionalidade",
        "Morada": "morada",
        "Código Postal": "codigo_postal",
        "Telefone": "telefone",
        "E-mail": "email"
    }

    for dl in soup_chamada6.find_all("dl"):
        dt = dl.find("dt")
        dd = dl.find("dd")
        if dt and dd:
            chave = dt.get_text(strip=True)
            if chave in campos_desejados:
                dados_cliente[campos_desejados[chave]] = dd.get_text(strip=True)

    # CHAMADA 7 - Autenticação no módulo contactos (SSO)
    response_chamada7 = session.get(
        'https://www.acesso.gov.pt/v2/loginForm?partID=CADP&path=/pessoal/contactos/dadosPessoais',
        headers={'User-Agent': 'Mozilla/5.0'},
        allow_redirects=False
    )

    soup_chamada7 = BeautifulSoup(response_chamada7.text, 'html.parser')

    js_model_chamada7_patterns = {
        'partID': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        'path': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        '_csrf_token_special': (CSRF_TOKEN_REGEX,),
        'action': (GENERIC_VALUE_REGEX_FUNC_FIRST, GENERIC_VALUE_REGEX_DIRECT_FIRST),
        'credential_json_parse': (JSON_PARSE_REGEX,) 
    }
    extracted_data_chamada7 = extract_js_data(soup_chamada7, 'var model =', js_model_chamada7_patterns)

    # Preparar dados para o POST da CHAMADA 8
    data_post_chamada8 = {}
    
    data_post_chamada8['_csrf'] = extracted_data_chamada7.get('_csrf_token_special')
    data_post_chamada8['path'] = extracted_data_chamada7.get('path')
    data_post_chamada8['partID'] = extracted_data_chamada7.get('partID')

    credential_data_chamada7 = extracted_data_chamada7.get('credential_json_parse', {})
    for key, value in credential_data_chamada7.items():
        data_post_chamada8[key] = value

    post_url_chamada8 = extracted_data_chamada7.get('action')
    if not post_url_chamada8:
        raise ValueError("URL de ação para POST da Chamada 8 não encontrado no script JavaScript.")

    # CHAMADA 8 - POST para autenticar acesso ao módulo de contactos
    response_chamada8 = session.post(
        post_url_chamada8,
        headers={
            'User-Agent': 'Mozilla/5.0',
            'Content-Type': 'application/x-www-form-urlencoded',
            'Origin': 'https://www.acesso.gov.pt',
            'Referer': 'https://www.acesso.gov.pt/',
        },
        data=data_post_chamada8,
        allow_redirects=False
    )

    # CHAMADA 9 - GET final para a página com os dados de contacto
    response_chamada9 = session.get(
        'https://sitfiscal.portaldasfinancas.gov.pt/pessoal/contactos/dadosPessoais',
        headers={
            'User-Agent': 'Mozilla/5.0',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation', # Ajustar Referer se vier de outro sitio
        }
    )

    soup_chamada9 = BeautifulSoup(response_chamada9.text, "html.parser")

    mapa_contactos = {
        "Telefone": "telefone_alt",
        "Telemóvel": "telemovel",
        "Email": "email_alt"
    }

    for dl in soup_chamada9.find_all("dl"):
        dt_tags = dl.find_all("dt")
        dd_tags = dl.find_all("dd")
        for dt, dd in zip(dt_tags, dd_tags):
            chave = dt.get_text(strip=True)
            if chave in mapa_contactos:
                dados_cliente[mapa_contactos[chave]] = dd.get_text(strip=True)

    
    result_json = json.dumps(dados_cliente, ensure_ascii=False)
    sys.stdout.buffer.write(result_json.encode('utf-8'))  # ← garante byte output puro em UTF-8

except Exception as e:
    print(f"Ocorreu um erro inesperado: {e}")
    sys.exit(1)
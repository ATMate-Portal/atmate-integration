import pickle
import requests
from bs4 import BeautifulSoup
import os
import re
import json # Importar para lidar com JSON aninhado
from urllib.parse import quote_plus

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
            if key == '_csrf_token_special':
                match = re.search(pattern, script_content, re.DOTALL)
                if match and match.group('token'):
                    value = match.group('token')
                    break
            elif key.endswith('_json_parse'): # Campo que contém um JSON string (ex: 'credential_json_parse')
                formatted_pattern = pattern.format(key=re.escape(key.replace('_json_parse', ''))) # Remove _json_parse para a regex
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

    session = requests.Session()

    # CHAMADA 1
    headers = {
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36',
    }
    response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard', headers=headers, allow_redirects=False)
    session.cookies.clear()

    # CHAMADA 2
    headers.update({'Sec-Fetch-Site': 'cross-site'})
    response = session.get('https://www.acesso.gov.pt/v2/loginForm?partID=PFAP&path=/geral/dashboard', headers=headers)

    # CHAMADA 3 - Extração de dados do JavaScript 'model' (primeiro HTML do login)
    soup_chamada3 = BeautifulSoup(response.text, 'html.parser')

    # Definição dos padrões para a extração do objeto 'model'
    js_model_patterns = {
        'path': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        'partID': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        'authVersion': (GENERIC_VALUE_REGEX_FUNC_FIRST, GENERIC_VALUE_REGEX_DIRECT_FIRST), # authVersion tem stringOrNull no teu HTML
        '_csrf_token_special': (CSRF_TOKEN_REGEX,)
    }

    extracted_js_data_model = extract_js_data(soup_chamada3, 'var model =', js_model_patterns)

    path = extracted_js_data_model.get('path')
    partID = extracted_js_data_model.get('partID')
    authVersion = extracted_js_data_model.get('authVersion')
    csrf_token = extracted_js_data_model.get('_csrf_token_special')


    # URL Encoding dos dados para o POST (CHAMADA 3)
    encoded_path = quote_plus(path)
    encoded_partID = quote_plus(partID)
    encoded_authVersion = quote_plus(authVersion)
    encoded_csrf_token = quote_plus(csrf_token)
    encoded_nif = quote_plus(nif)
    encoded_password = quote_plus(password)

    data_post_login = (
        f'path={encoded_path}'
        f'&partID={encoded_partID}'
        f'&authVersion={encoded_authVersion}'
        f'&_csrf={encoded_csrf_token}'
        f'&selectedAuthMethod=N'
        f'&username={encoded_nif}'
        f'&password={encoded_password}'
    )

    headers.update({
        'Content-Type': 'application/x-www-form-urlencoded',
        'Origin': 'https://www.acesso.gov.pt',
        'Referer': 'https://www.acesso.gov.pt/v2/loginForm?partID=PFAP&path=/geral/dashboard',
    })

    response_post_login = session.post('https://www.acesso.gov.pt/v2/login', data=data_post_login, headers=headers)

    # CHAMADA 4 - Extração de dados do SSO (AGORA TAMBÉM DO MESMO OBJETO 'model' NO NOVO HTML)
    soup_chamada4 = BeautifulSoup(response_post_login.text, 'html.parser')

    # O "model" da CHAMADA 4 tem os dados que precisas em 'credential', que é um JSON.parse
    js_sso_patterns = {
        # Campo 'action' para onde ir depois
        'action': (GENERIC_VALUE_REGEX_FUNC_FIRST, GENERIC_VALUE_REGEX_DIRECT_FIRST),
        # 'credential' é um campo especial, que tem um JSON string dentro
        'credential_json_parse': (JSON_PARSE_REGEX,) # Esta chave será processada como JSON
    }

    # Assumindo que os dados SSO estão no mesmo objeto 'model' do HTML da Chamada 4
    extracted_js_data_sso = extract_js_data(soup_chamada4, 'var model =', js_sso_patterns)

    # Extrair os campos individuais do dicionário 'credential' que foi parseado
    sso_data = extracted_js_data_sso.get('credential_json_parse', {})
    
    # Adicionar o campo 'action' ao sso_data, se existir
    if 'action' in extracted_js_data_sso:
        # Nota: O 'action' que está aqui é 'https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard'
        # que é o URL POST. Se o formulário tiver um 'action' oculto, tem que vir para aqui.
        # Caso contrário, 'data_post_sso' passará o dicionário e 'requests' gere o URL de POST.
        pass # Não precisamos de o adicionar ao sso_data, o URL de POST já está no response = session.post()

    # O 'data_post_sso' é o dicionário de campos {name: value}
    # requests.post() pode enviar um dicionário diretamente, e ele fará o encoding.
    data_post_sso = sso_data 

    headers.update({
        'Referer': 'https://www.acesso.gov.pt',
        'Sec-Fetch-Site': 'cross-site',
    })

    cookie_value = session.cookies.get('autentica_JSessionID', None)
    if not cookie_value:
        raise ValueError("Erro: Cookie 'autentica_JSessionID' não foi encontrada.")

    with open(jsessionid_file_path, 'wb') as f:
        pickle.dump(cookie_value, f)
    print(f"Cookie 'autentica_JSessionID' salvo em {jsessionid_file_path}")

    session.cookies.clear() # Limpar cookies antes do próximo POST, conforme original

    # O URL de POST para a Chamada 4 (SSO) vem da variável 'action' do model
    # response_post_sso = session.post('URL_DE_DESTINO_DO_POST_SSO', data=data_post_sso, headers=headers, allow_redirects=False)
    # No seu HTML, 'action' é 'https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard'
    response_post_sso = session.post(extracted_js_data_sso['action'], data=data_post_sso, headers=headers, allow_redirects=False)


    # CHAMADA FINAL
    response_final = session.get('https://sitfiscal.portaldasfinancas.gov.pt/geral/home', cookies=session.cookies, headers=headers)

    if response_final.status_code == 200:
        try:
            with open(session_file_path, 'wb') as f:
                pickle.dump(session, f)
            print("Sessão salva com sucesso.")
        except Exception as e:
            print(f"Erro ao salvar a sessão: {e}")
    else:
        print(f"Erro ao comunicar com o sistema. Status code: {response_final.status_code}")

except Exception as e:
    print(f"Ocorreu um erro inesperado: {e}")
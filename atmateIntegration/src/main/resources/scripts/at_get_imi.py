import pickle
import requests
from bs4 import BeautifulSoup
import json
import os
import re # Necessário para as regexes
from urllib.parse import quote_plus # Necessário para URL encoding
import sys # Para output UTF-8 puro

# --- Regexes Comuns para Extração de Valores de JavaScript (reutilizadas) ---
GENERIC_VALUE_REGEX_DIRECT_FIRST = r"{key}:\s*(?:'([^']*)'|stringOrNull\('([^']*)'\))"
GENERIC_VALUE_REGEX_FUNC_FIRST = r"{key}:\s*(?:stringOrNull\('([^']*)'\)|'([^']*)')"
CSRF_TOKEN_REGEX = r"_csrf:\s*\{\s*parameterName:\s*'[^']*',?\s*token\s*:\s*'(?P<token>[^']+)',?.*?\}"
JSON_PARSE_REGEX = r"{key}:\s*JSON\.parse\('(.+?)'\s*\|\|\s*null\)"

# --- Função Auxiliar de Extração de JS (reutilizada) ---
def extract_js_data(html_soup, script_identifier, field_patterns):
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
            if pattern is None: continue
            
            if key == '_csrf_token_special':
                match = re.search(pattern, script_content, re.DOTALL)
                if match and match.group('token'):
                    value = match.group('token')
                    break
            elif key.endswith('_json_parse'):
                regex_key = key.replace('_json_parse', '')
                formatted_pattern = pattern.format(key=re.escape(regex_key)) 
                match = re.search(formatted_pattern, script_content, re.DOTALL)
                if match:
                    json_str = match.group(1)
                    if json_str:
                        try:
                            value = json.loads(json_str)
                            break
                        except json.JSONDecodeError:
                            pass
            else:
                formatted_pattern = pattern.format(key=re.escape(key))
                match = re.search(formatted_pattern, script_content)
                if match:
                    value = match.group(1) if match.group(1) else (match.group(2) if len(match.groups()) >= 2 else None)
                    if value is not None:
                        break

        if value is not None:
            extracted_data[key] = value
        else:
            # Não levantar erro para 'credential_json_parse' se for permitido que esteja vazio
            if key == 'credential_json_parse':
                extracted_data[key] = {}
            else:
                raise ValueError(f"Valor '{key}' não encontrado no script JavaScript identificado por '{script_identifier}'.")
            
    return extracted_data


try:
    nif = os.environ.get("NIF")
    if not nif:
        nif = "226144275"
    scriptPath = os.environ.get("SCRIPT_PATH")
    if not scriptPath:
        scriptPath = "src/main/resources/scripts/"

    session_file_name = f'session_{nif}.pkl'
    jsessionid_file_name = f'JSessionID_{nif}.pkl'

    session_file_path = os.path.join(scriptPath, session_file_name)
    jsessionid_file_path = os.path.join(scriptPath, jsessionid_file_name)

    # Carregar a sessão salva
    with open(session_file_path, 'rb') as f:
        session = pickle.load(f)

    # CHAMADA 1
    headers_chamada1 = { # Headers redefinidos para cada bloco de chamada
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'pt-PT,pt;q=0.8',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Pragma': 'no-cache',
        'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'same-site',
        'Sec-Fetch-User': '?1',
        'Sec-GPC': '1',
        'Upgrade-Insecure-Requests': '1',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36',
        'sec-ch-ua': '"Brave";v="135", "Not-A.Brand";v="8", "Chromium";v="135"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
    }

    response_chamada1 = session.get('https://www.portaldasfinancas.gov.pt/main.jsp?body=/ca/notasCobrancaForm.jsp', cookies=session.cookies, headers=headers_chamada1, allow_redirects=False)

    # CHAMADA 2
    location_chamada2 = response_chamada1.headers.get('Location')
    if not location_chamada2:
        raise ValueError("Location header não encontrado na Chamada 1 para o redirecionamento da Chamada 2.")

    response_chamada2 = session.get(location_chamada2, cookies=session.cookies, headers=headers_chamada1) # Reutilizar headers da Chamada 1


    # CHAMADA 3
    headers_chamada3 = headers_chamada1.copy() # Copia os headers da Chamada 1/2
    headers_chamada3.update({
        'Sec-Fetch-Site': 'cross-site',
        'Referer': 'https://www.portaldasfinancas.gov.pt/',
    })

    with open(jsessionid_file_path, 'rb') as f:
        cookie_value = pickle.load(f)
    session.cookies.set('autentica_JSessionID', cookie_value)

    params_chamada3 = {
        'partID': 'PFIN',
        'path': 'main.jsp?body=/ca/notasCobrancaForm.jsp',
    }

    response_chamada3 = session.get('https://www.acesso.gov.pt/loginRedirectForm', params=params_chamada3, cookies=session.cookies, headers=headers_chamada3)


    # CHAMADA 4 - Extrair dados do JavaScript 'model' (do HTML da Chamada 3)
    soup_chamada4_source = BeautifulSoup(response_chamada3.text, 'html.parser') # A resposta da CHAMADA 3 é que tem o HTML com o 'model'

    # Definição dos padrões para a extração do objeto 'model' da CHAMADA 4
    # Este HTML deve ser similar ao que já vimos para 'acesso.gov.pt', mas com partID='PFIN'
    # e o credential_json_parse deve ter os dados que antes eram inputs
    js_model_chamada4_patterns = {
        'partID': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        'path': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        '_csrf_token_special': (CSRF_TOKEN_REGEX,),
        'action': (GENERIC_VALUE_REGEX_FUNC_FIRST, GENERIC_VALUE_REGEX_DIRECT_FIRST), # 'action' é o URL POST
        'credential_json_parse': (JSON_PARSE_REGEX,) # Este JSON deve conter tv, sign, nif, sessionID, etc.
    }

    extracted_data_chamada4 = extract_js_data(soup_chamada4_source, 'var model =', js_model_chamada4_patterns)

    # Preparar o payload 'data_post_chamada4' a partir dos dados extraídos do model
    data_post_chamada4 = extracted_data_chamada4.get('credential_json_parse', {})

    # Adicionar o _csrf, path, partID (se não estiverem já no credential)
    if '_csrf_token_special' in extracted_data_chamada4:
        data_post_chamada4['_csrf'] = extracted_data_chamada4['_csrf_token_special']
    if 'path' in extracted_data_chamada4 and 'path' not in data_post_chamada4:
        data_post_chamada4['path'] = extracted_data_chamada4['path']
    if 'partID' in extracted_data_chamada4 and 'partID' not in data_post_chamada4:
        data_post_chamada4['partID'] = extracted_data_chamada4['partID']

    post_url_chamada4 = extracted_data_chamada4.get('action') # O URL para o POST vem do campo 'action' do model
    if not post_url_chamada4:
        raise ValueError("URL de ação para POST da Chamada 4 não encontrado no script JavaScript.")

    headers_chamada4 = { # Redefinição de headers para a Chamada 4 POST
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'pt-PT,pt;q=0.8',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Content-Type': 'application/x-www-form-urlencoded',
        'Origin': 'https://www.acesso.gov.pt', # Corrigido para acesso.gov.pt
        'Pragma': 'no-cache',
        'Referer': 'https://www.acesso.gov.pt/', # Corrigido para acesso.gov.pt
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'cross-site',
        'Sec-GPC': '1',
        'Upgrade-Insecure-Requests': '1',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36',
        'sec-ch-ua': '"Brave";v="135", "Not-A.Brand";v="8", "Chromium";v="135"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
    }

    response_chamada4 = session.post(
        post_url_chamada4, # URL dinâmico
        cookies=session.cookies,
        headers=headers_chamada4,
        data=data_post_chamada4, # Dados como dicionário
        allow_redirects=False
    )

    # CHAMADA 5
    response_chamada5 = session.get(
        'https://www.portaldasfinancas.gov.pt/pt/main.jsp?body=/ca/notasCobrancaForm.jsp', # URL ajustado para /pt/main.jsp
        cookies=session.cookies,
        headers=headers_chamada4, # Reutilizar headers da Chamada 4 POST
    )

    # CHAMADA 6
    headers_chamada6 = headers_chamada4.copy() # Copia os headers da Chamada 4
    headers_chamada6.update({
        'Referer': 'https://www.portaldasfinancas.gov.pt/pt/main.jsp?body=/ca/notasCobrancaForm.jsp', # Referer atualizado
    })

    data_post_chamada6 = {
        'body': '/ca/notasCobrancaIMI.jsp',
        'ano': '2023',
    }

    response_chamada6 = session.post('https://www.portaldasfinancas.gov.pt/pt/main.jsp', cookies=session.cookies, headers=headers_chamada6, data=data_post_chamada6)


    soup_chamada6 = BeautifulSoup(response_chamada6.text, 'html.parser')

    mainTables = soup_chamada6.find_all('table', class_='eT')

    tabela_eT_correta = None
    for tabela_eT in mainTables:
        if tabela_eT.find('table', class_='iT'):
            tabela_eT_correta = tabela_eT
            break

    if not tabela_eT_correta: # Adicionar verificação se a tabela principal não for encontrada
        raise ValueError("Tabela principal 'eT' com 'iT' aninhada não encontrada na Chamada 6.")

    tabela_iT = tabela_eT_correta.find('table', class_='iT')

    if not tabela_iT: # Adicionar verificação se a tabela aninhada não for encontrada
        raise ValueError("Tabela 'iT' aninhada não encontrada na Chamada 6.")


    headers_table = [th.get_text(strip=True) for th in tabela_iT.find('tr', class_='iTR').find_all('th')[:-1]]

    data_table_rows = []

    for row in tabela_iT.find_all('tr', class_='iTR'):
        cols = row.find_all('td')[:-1]
        if cols:
            row_data = [col.get_text(strip=True).replace("\xa0", " ").replace("€", "EUR") for col in cols]
            data_table_rows.append(row_data)

    result_final_cobranca = {"headers": headers_table, "rows": data_table_rows} # Renomeado para evitar conflito

    result_json = json.dumps(result_final_cobranca, ensure_ascii=False).encode('utf-8').decode('utf-8')
    sys.stdout.buffer.write(result_json.encode('utf-8'))

except Exception as e:
    print(f"Ocorreu um erro inesperado: {e}")
    sys.exit(1)
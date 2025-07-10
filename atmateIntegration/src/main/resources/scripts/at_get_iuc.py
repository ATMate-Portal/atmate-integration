import pickle
import requests
from bs4 import BeautifulSoup
import json
import os
import re
from urllib.parse import quote_plus
import sys

# --- Regexes Comuns para Extração de Valores de JavaScript ---
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
            if key == 'credential_json_parse': # Permitir que credential esteja vazio
                extracted_data[key] = {} # Define como dicionário vazio em vez de erro
            else:
                raise ValueError(f"Valor '{key}' não encontrado no script JavaScript identificado por '{script_identifier}'.")
            
    return extracted_data


try:
    getTypeFromAT = os.environ.get("getTypeFromAT")
    if not getTypeFromAT:
        getTypeFromAT = True # Convert to boolean later if used as boolean

    nif = os.environ.get("NIF")
    if not nif:
        nif = ""
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

    # Adicionar cookie "autentica_JSessionID" guardada previamente
    with open(jsessionid_file_path, 'rb') as f:
        cookie_value = pickle.load(f)
    session.cookies.set('autentica_JSessionID', cookie_value)

    # CHAMADA 1
    headers = {
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'pt-PT,pt;q=0.7',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Pragma': 'no-cache',
        'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'same-origin',
        'Sec-Fetch-User': '?1',
        'Sec-GPC': '1',
        'Upgrade-Insecure-Requests': '1',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36',
        'sec-ch-ua': '"Not(A:Brand";v="99", "Brave";v="133", "Chromium";v="133"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
    }

    response_chamada1 = session.get('https://sitfiscal.portaldasfinancas.gov.pt/iuc', cookies=session.cookies, headers=headers, allow_redirects=False)

    # CHAMADA 2
    location = response_chamada1.headers.get('Location')
    if not location:
        raise ValueError("Location header não encontrado na Chamada 1.")

    headers.update({'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/'})

    response_chamada2 = session.get(location, cookies=session.cookies, headers=headers)

    # CHAMADA 3 - Construção do Payload (data) do HTML DA CHAMADA 2
    soup_chamada3_source = BeautifulSoup(response_chamada2.text, 'html.parser')

    # ---- NOVO: Extração de todos os dados do 'model' na Chamada 2 ----
    # O HTML da Chamada 2 que forneceste tem um 'model' com 'credential: JSON.parse('' || null)'
    # mas também tem os campos 'partID', 'path', '_csrf', 'action'
    js_model_chamada3_patterns = {
        'partID': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        'path': (GENERIC_VALUE_REGEX_DIRECT_FIRST, GENERIC_VALUE_REGEX_FUNC_FIRST),
        '_csrf_token_special': (CSRF_TOKEN_REGEX,),
        'action': (GENERIC_VALUE_REGEX_FUNC_FIRST, GENERIC_VALUE_REGEX_DIRECT_FIRST),
        'credential_json_parse': (JSON_PARSE_REGEX,) # Este pode estar vazio. A função já foi ajustada para lidar com isso.
    }
    extracted_data_from_model_chamada3 = extract_js_data(soup_chamada3_source, 'var model =', js_model_chamada3_patterns)

    # Construir o payload 'data_post_chamada3'
    data_post_chamada3 = {}
    
    # Adicionar campos principais do model (partID, path, _csrf)
    data_post_chamada3['partID'] = extracted_data_from_model_chamada3.get('partID')
    data_post_chamada3['path'] = extracted_data_from_model_chamada3.get('path')
    data_post_chamada3['_csrf'] = extracted_data_from_model_chamada3.get('_csrf_token_special')
    
    # Adicionar campos do 'credential' (se existirem)
    credential_payload = extracted_data_from_model_chamada3.get('credential_json_parse', {})
    for key, value in credential_payload.items():
        data_post_chamada3[key] = value

    # Adicionar o URL de POST
    post_url_chamada3 = extracted_data_from_model_chamada3.get('action')
    if not post_url_chamada3:
        raise ValueError("URL de ação para POST da Chamada 3 não encontrado no script JavaScript.")


    headers.update({
        'Content-Type': 'application/x-www-form-urlencoded',
        'Origin': 'https://www.acesso.gov.pt',
        'Referer': 'https://www.acesso.gov.pt',
        'Sec-Fetch-Site': 'cross-site',
    })

    response_chamada3 = session.post(post_url_chamada3, cookies=session.cookies, headers=headers, data=data_post_chamada3, allow_redirects=False)

    # CHAMADA 4
    response_chamada4 = session.get('https://sitfiscal.portaldasfinancas.gov.pt/iuc', cookies=session.cookies, headers=headers)

    # CHAMADA 5
    headers_chamada5 = { # Redefinição de headers para a Chamada 5
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'pt-PT,pt;q=0.8',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Content-Type': 'application/x-www-form-urlencoded',
        'Origin': 'https://sitfiscal.portaldasfinancas.gov.pt',
        'Pragma': 'no-cache',
        'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/iuc',
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

    soup_chamada5_initial = BeautifulSoup(response_chamada4.text, 'html.parser')

    # Para a CHAMADA 5, o '_csrf' é o único input.
    # Tentativa 1: Procurar _csrf como input HTML (como no original)
    _csrf_chamada5_input = soup_chamada5_initial.find("input", {"name": "_csrf"})
    if _csrf_chamada5_input and "value" in _csrf_chamada5_input.attrs:
        csrf_token_chamada5 = _csrf_chamada5_input["value"]
    else: # Tentativa 2: Procurar _csrf em um 'model' object
        js_model_chamada5_patterns = {
            '_csrf_token_special': (CSRF_TOKEN_REGEX,),
        }
        try:
            # Assumindo que o _csrf pode estar em 'var model =' nesta página
            extracted_model_chamada5 = extract_js_data(soup_chamada5_initial, 'var model =', js_model_chamada5_patterns)
            csrf_token_chamada5 = extracted_model_chamada5.get('_csrf_token_special')
        except ValueError:
            raise ValueError("CSRF Token para Chamada 5 não encontrado (nem input nem em script model).")

    data_post_chamada5 = {
        '_csrf': csrf_token_chamada5,
    }

    response_chamada5_part1 = session.post( # Renomeado para part1
        'https://sitfiscal.portaldasfinancas.gov.pt/iuc/consultarIUC/consultarIUC',
        cookies=session.cookies,
        headers=headers_chamada5, # Usar os headers específicos da Chamada 5
        data=data_post_chamada5)


    data_post_chamada5_part2 = data_post_chamada5.copy()
    data_post_chamada5_part2.update({
        'action': 'consultaVeiculos',
        'ano': '',
    })

    response_chamada5_part2 = session.post(
        'https://sitfiscal.portaldasfinancas.gov.pt/iuc/consultarIUC/consultaIUCANO',
        cookies=session.cookies,
        headers=headers_chamada5, # Usar os headers específicos da Chamada 5
        data=data_post_chamada5_part2,
    )

    soup_chamada5_part2 = BeautifulSoup(response_chamada5_part2.text, 'html.parser')

    button_elements = soup_chamada5_part2.findAll('button', {'id': 'btnConsultVeic'})

    if not button_elements: # Adicionar verificação se botões não forem encontrados
        button_values = []
    else:
        button_values = [button.get('value') for button in button_elements]

    table = soup_chamada5_part2.find('table', {'id': 'LST_VEICULOS_CONSULTA_ID'})

    if not table:
        # Se a tabela não for encontrada, significa que a estrutura da página mudou ou não há dados
        headers_table = []
        data_table_rows = []
    else:
        headers_table = [th.get_text(strip=True) for th in table.find('thead').find_all('th')[:-1]]
        data_table_rows = []
        for row in table.find('tbody').find_all('tr'):
            cols = row.find_all('td')[:-1]
            row_data = [col.get_text(strip=True).replace("\xa0", " ").replace("€", "EUR") for col in cols]
            data_table_rows.append(row_data)

    result_iuc_summary = {"headers": headers_table, "rows": data_table_rows}

    all_vehicle_details = []

    headers_vehicle_details = {
        'Accept': 'application/json, text/javascript, */*; q=0.01',
        'Accept-Language': 'pt-PT,pt;q=0.8',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Content-Type': 'application/json; charset=utf-8',
        'Pragma': 'no-cache',
        'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/iuc/consultarIUC/consultaIUCANO',
        'Sec-Fetch-Dest': 'empty',
        'Sec-Fetch-Mode': 'cors',
        'Sec-Fetch-Site': 'same-origin',
        'Sec-GPC': '1',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36',
        'X-Requested-With': 'XMLHttpRequest',
        'sec-ch-ua': '"Brave";v="135", "Not-A.Brand";v="8", "Chromium";v="135"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
    }

    for button_value in button_values:
        params_vehicle_details = {
            'matriculaValue': button_value,
        }

        response_vehicle_details = session.get(
            'https://sitfiscal.portaldasfinancas.gov.pt/iuc/comum/consultaVeicMatricula',
            params=params_vehicle_details,
            cookies=session.cookies,
            headers=headers_vehicle_details,
        )

        if response_vehicle_details.status_code == 200 and 'application/json' in response_vehicle_details.headers.get('Content-Type', ''):
            try:
                vehicle_details = response_vehicle_details.json()
                vehicle_details['matriculaValue'] = button_value
            except json.JSONDecodeError:
                vehicle_details = {"error": "Falha ao obter detalhes do veículo", "matriculaValue": button_value, "raw_response": response_vehicle_details.text}
        else:
            vehicle_details = {"error": "Falha na requisição", "matriculaValue": button_value, "status_code": response_vehicle_details.status_code}

        all_vehicle_details.append(vehicle_details)

    final_combined_data = {
        "detalhes_veiculos": all_vehicle_details,
        "resumo_iuc": result_iuc_summary
    }

    result_json = json.dumps(final_combined_data, ensure_ascii=False).encode('utf-8').decode('utf-8')
    print(result_json)

except Exception as e:
    print(f"Ocorreu um erro inesperado: {e}")
    sys.exit(1)
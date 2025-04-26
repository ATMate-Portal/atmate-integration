import pickle
from bs4 import BeautifulSoup
import json
import os
import sys

nif = os.environ.get("NIF")
if not nif:
    nif = "248102931"
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
session.get(
    'https://sitfiscal.portaldasfinancas.gov.pt/integrada',
    headers={
        'User-Agent': 'Mozilla/5.0',
        'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard',
    },
    allow_redirects=False
)

# CHAMADA 2 - Obter formulário da SSO
response = session.get(
    'https://www.acesso.gov.pt/v2/loginForm?partID=SFIC&path=/integrada',
    headers={'User-Agent': 'Mozilla/5.0'},
    allow_redirects=False
)
soup = BeautifulSoup(response.text, 'html.parser')

data = {campo['name']: campo['value'] for campo in soup.find_all('input', {'name': True, 'value': True})}

# CHAMADA 3 - POST para autenticar acesso ao módulo integrada
response = session.post(
    'https://sitfiscal.portaldasfinancas.gov.pt/integrada',
    headers={
        'User-Agent': 'Mozilla/5.0',
        'Content-Type': 'application/x-www-form-urlencoded',
        'Origin': 'https://www.acesso.gov.pt',
        'Referer': 'https://www.acesso.gov.pt/',
    },
    data=data,
    allow_redirects=False
)

# CHAMADA 4 - Redirecionamento para /integrada
redirect_url = response.headers.get("Location")
if redirect_url and redirect_url.startswith("/"):
    redirect_url = "https://sitfiscal.portaldasfinancas.gov.pt" + redirect_url

session.get(redirect_url, headers={'User-Agent': 'Mozilla/5.0'})

# CHAMADA 5 - GET para presentation?httpRefererTransId=...
session.get(
    'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation',
    params={
        'httpRefererTransId': 'e2383a35-00ce-44ea-8ff5-97c9918d6ce2'  # Este valor pode variar dinamicamente
    },
    headers={'User-Agent': 'Mozilla/5.0'}
)

soup = BeautifulSoup(response.text, "html.parser")

# Dicionário final a devolver
dados_cliente = {}

# Adicionar a informação da "Atividade Exercida" ao dicionário, se getTypeFromAT for True
if getTypeFromAT:
    atividade_exercida_encontrada = "Atividade Exercida" in soup.get_text()
    dados_cliente["atividade_exercida_encontrada"] = atividade_exercida_encontrada

    if atividade_exercida_encontrada:
        # Find the <a> tag with "Atividade Exercida" and extract href
        atividade_link = soup.find("a", title="Consultar Atividade Exercida")
        if atividade_link and atividade_link.get("href"):
            # Construct the full URL
            base_url = "https://sitfiscal.portaldasfinancas.gov.pt/integrada/"
            full_url = base_url + atividade_link["href"]

            # Headers for the request
            headers = {
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
                'Accept-Language': 'pt-PT,pt;q=0.8',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive',
                'Pragma': 'no-cache',
                'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation?httpRefererTransId=93b3e40f-44fd-4cea-b872-454570c1c858',
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

            # Make the request using the extracted URL
            response = session.get(
                full_url,
                cookies=session.cookies,  # Ensure cookies is defined
                headers=headers,
            )

            # Parse the response
            soup_response = BeautifulSoup(response.text, "html.parser")

            # Find the "Data de Cessação" element
            cessacao_element = soup_response.find("dt", string="Data de Cessação")
            if cessacao_element:
                cessacao_date = cessacao_element.find_next("dd").get_text(strip=True)
                dados_cliente["data_cessacao"] = cessacao_date if cessacao_date != "-" else None
            else:
                dados_cliente["data_cessacao"] = None

# CHAMADA 6 - GET final com queryStringS e hmac
response = session.get(
    'https://sitfiscal.portaldasfinancas.gov.pt/integrada/presentation',
    params={
        'queryStringS': 'targetScreen=ecraIdentificacao&hmac=z1DA4exOnazVrAB2PGbKeRYtiOk='
    },
    headers={'User-Agent': 'Mozilla/5.0'}
)

soup = BeautifulSoup(response.text, "html.parser")

# Mapeamento dos campos que queres extrair
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

# Dicionário final a devolver
dados_cliente = {}

# Iterar sobre todos os <dl> e preencher o dicionário
for dl in soup.find_all("dl"):
    dt = dl.find("dt")
    dd = dl.find("dd")
    if dt and dd:
        chave = dt.get_text(strip=True)
        if chave in campos_desejados:
            dados_cliente[campos_desejados[chave]] = dd.get_text(strip=True)

# Exemplo de uso
result_json = json.dumps(dados_cliente, ensure_ascii=False)
sys.stdout.buffer.write(result_json.encode('utf-8'))  # ← garante byte output puro em UTF-8
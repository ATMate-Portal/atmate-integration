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
getTypeFromAT = os.environ.get("getTypeFromAT")
if not getTypeFromAT:
    getTypeFromAT = "false"


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
response = session.get(
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
import pickle
from bs4 import BeautifulSoup
import json
import os

nif = os.environ.get("NIF")
if not nif:
    nif = "249428520"
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

#CHAMADA 1
################################################################################################################################
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

response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/iuc', cookies=session.cookies, headers=headers, allow_redirects=False)
################################################################################################################################
#CHAMADA 2
################################################################################################################################

with open(jsessionid_file_path, 'rb') as f:
    cookie_value = pickle.load(f)
# Adicionar a cookie manualmente à sessão
session.cookies.set('autentica_JSessionID', cookie_value)

location = response.headers.get('Location')

headers.update({'Referer': 'https://sitfiscal.portaldasfinancas.gov.pt/'}) #Só muda este header

response = session.get(location, cookies=session.cookies, headers=headers)
################################################################################################################################
#CHAMADA 3
################################################################################################################################
# Construção do Payload (data)
soup = BeautifulSoup(response.text, 'html.parser')

data = {
    "ssoID": soup.find("input", {"name": "ssoID"})["value"],
    "signParameters": soup.find("input", {"name": "signParameters"})["value"],
    "tv": soup.find("input", {"name": "tv"})["value"],
    "idType": soup.find("input", {"name": "idType"})["value"],
    "partID": soup.find("input", {"name": "partID"})["value"],
    "sign": soup.find("input", {"name": "sign"})["value"],
    "userName": soup.find("input", {"name": "userName"})["value"],
    "userID": soup.find("input", {"name": "userID"})["value"],
    "tc": soup.find("input", {"name": "tc"})["value"],
    "credentialID": soup.find("input", {"name": "credentialID"})["value"],
    "id": soup.find("input", {"name": "id"})["value"],
    "authMethod": soup.find("input", {"name": "authMethod"})["value"],
    "authQAALevel": soup.find("input", {"name": "authQAALevel"})["value"],
}

headers.update({
    'Content-Type': 'application/x-www-form-urlencoded',
    'Origin': 'https://www.acesso.gov.pt',
    'Referer': 'https://www.acesso.gov.pt',
    'Sec-Fetch-Site': 'cross-site',
})

response = session.post('https://sitfiscal.portaldasfinancas.gov.pt/iuc', cookies=session.cookies, headers=headers, data=data, allow_redirects=False)
################################################################################################################################
#CHAMADA 4
################################################################################################################################

response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/iuc', cookies=session.cookies, headers=headers)

################################################################################################################################
#CHAMADA 5
################################################################################################################################

data = {
    'action': 'consultaVeiculos',
    'ano': '',
}

response = session.post(
    'https://sitfiscal.portaldasfinancas.gov.pt/iuc/consultarIUC/consultaIUCANO',
    cookies=session.cookies,
    headers=headers,
    data=data,
)

# Criar objeto BeautifulSoup
soup = BeautifulSoup(response.text, 'html.parser')

# Encontrar a tabela
table = soup.find('table', {'id': 'LST_VEICULOS_CONSULTA_ID'})

# Obter os headers
headers = [th.get_text(strip=True) for th in table.find('thead').find_all('th')[:-1]]  # Remove a última coluna

# Inicializar lista para armazenar os dados das linhas
data = []

# Iterar sobre todas as linhas <tr> dentro do <tbody>
for row in table.find('tbody').find_all('tr'):
    cols = row.find_all('td')[:-1]  # Remove a última coluna
    row_data = [col.get_text(strip=True).replace("\xa0", " ").replace("€", "EUR") for col in cols]
    data.append(row_data)

# Criar dicionário com headers e dados
result = {"headers": headers, "rows": data}

result_json = json.dumps(result, ensure_ascii=False).encode('utf-8').decode('utf-8')
print(result_json)





#for cookie in session.cookies:
#print(f'Nome: {cookie.name}, Valor: {cookie.value}')


import pickle
from bs4 import BeautifulSoup
import json
import os

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

################################################################################################################################
#CHAMADA 1
################################################################################################################################


headers = {
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

response = session.get('https://www.portaldasfinancas.gov.pt/main.jsp?body=/ca/notasCobrancaForm.jsp', cookies=session.cookies, headers=headers, allow_redirects=False)

################################################################################################################################
#CHAMADA 2
################################################################################################################################

location = response.headers.get('Location')

response = session.get(location, cookies=session.cookies, headers=headers)

################################################################################################################################
#CHAMADA 3
################################################################################################################################

headers.update({
    'Sec-Fetch-Site': 'cross-site',
    'Referer': 'https://www.portaldasfinancas.gov.pt/',
})

with open(jsessionid_file_path, 'rb') as f:
    cookie_value = pickle.load(f)
# Adicionar a cookie manualmente à sessão
session.cookies.set('autentica_JSessionID', cookie_value)

params = {
    'partID': 'PFIN',
    'path': 'main.jsp?body=/ca/notasCobrancaForm.jsp',
}

response = session.get('https://www.acesso.gov.pt/loginRedirectForm', params=params, cookies=session.cookies, headers=headers)

################################################################################################################################
#CHAMADA 4
################################################################################################################################


soup = BeautifulSoup(response.text, 'html.parser')

headers = {
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'pt-PT,pt;q=0.8',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive',
    'Content-Type': 'application/x-www-form-urlencoded',
    'Origin': 'https://www.acesso.gov.pt',
    'Pragma': 'no-cache',
    'Referer': 'https://www.acesso.gov.pt/',
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

data = {
    'tv': soup.find("input", {"name": "tv"})["value"],
    'partID': soup.find("input", {"name": "partID"})["value"],
    'sign': soup.find("input", {"name": "sign"})["value"],
    'nif': soup.find("input", {"name": "nif"})["value"],
    'sessionID': soup.find("input", {"name": "sessionID"})["value"],
    'userName': soup.find("input", {"name": "userName"})["value"],
    'userID': soup.find("input", {"name": "userID"})["value"],
    'tc': soup.find("input", {"name": "tc"})["value"],
}

response = session.post(
    'https://www.portaldasfinancas.gov.pt/main.jsp?body=/ca/notasCobrancaForm.jsp',
    cookies=session.cookies,
    headers=headers,
    data=data,
    allow_redirects=False
)

################################################################################################################################
#CHAMADA 5
################################################################################################################################

response = session.get(
    'https://www.portaldasfinancas.gov.pt/pt/main.jsp?body=/ca/notasCobrancaForm.jsp',
    cookies=session.cookies,
    headers=headers,
)

################################################################################################################################
#CHAMADA 6
################################################################################################################################


headers = {
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'pt-PT,pt;q=0.8',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive',
    'Content-Type': 'application/x-www-form-urlencoded',
    'Origin': 'https://www.portaldasfinancas.gov.pt',
    'Pragma': 'no-cache',
    'Referer': 'https://www.portaldasfinancas.gov.pt/pt/main.jsp?body=/ca/notasCobrancaForm.jsp',
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

data = {
    'body': '/ca/notasCobrancaIMI.jsp',
    'ano': '2024',
}

response = session.post('https://www.portaldasfinancas.gov.pt/pt/main.jsp', cookies=session.cookies, headers=headers, data=data)


soup = BeautifulSoup(response.text, 'html.parser')

mainTables = soup.find_all('table', class_='eT')

tabela_eT_correta = None
for tabela_eT in mainTables:
    if tabela_eT.find('table', class_='iT'):
        tabela_eT_correta = tabela_eT
        break

tabela_iT = tabela_eT_correta.find('table', class_='iT')

# Obter os headers (CORREÇÃO: remover a última coluna aqui também)
headers = [th.get_text(strip=True) for th in tabela_iT.find('tr', class_='iTR').find_all('th')[:-1]]

# Inicializar lista para armazenar os dados das linhas
data = []

# Iterar sobre todas as linhas <tr> com a classe 'iTR' dentro da tabela 'iT'
for row in tabela_iT.find_all('tr', class_='iTR'):
    cols = row.find_all('td')[:-1]  # Remove a última coluna
    if cols:  # Verifica se a lista de colunas não está vazia
        row_data = [col.get_text(strip=True).replace("\xa0", " ").replace("€", "EUR") for col in cols]
        data.append(row_data)

# Criar dicionário com headers e dados
result = {"headers": headers, "rows": data}

result_json = json.dumps(result, ensure_ascii=False).encode('utf-8').decode('utf-8')
print(result_json)
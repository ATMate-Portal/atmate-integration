import pickle
import requests
from bs4 import BeautifulSoup

# Criar uma sessão para gerenciar cookies automaticamente
session = requests.Session()

#CHAMADA 1
################################################################################################################################
headers = {
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'pt-PT,pt;q=0.8',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive',
    'Pragma': 'no-cache',
    'Referer': 'https://www.portaldasfinancas.gov.pt/',
    'Sec-Fetch-Dest': 'document',
    'Sec-Fetch-Mode': 'navigate',
    'Sec-Fetch-Site': 'same-site',
    'Sec-Fetch-User': '?1',
    'Sec-GPC': '1',
    'Upgrade-Insecure-Requests': '1',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36',
    'sec-ch-ua': '"Not(A:Brand";v="99", "Brave";v="133", "Chromium";v="133"',
    'sec-ch-ua-mobile': '?0',
    'sec-ch-ua-platform': '"Windows"',
}
response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard', headers=headers, allow_redirects=False)
session.cookies.clear() # Não são necessárias as cookies desta resposta
################################################################################################################################

#CHAMADA 2
################################################################################################################################
headers.update({'Sec-Fetch-Site': 'cross-site'}) #Só muda este header

# Esta chamada não precisa de cookies.
response = session.get('https://www.acesso.gov.pt/v2/loginForm?partID=PFAP&path=/geral/dashboard', headers=headers)
################################################################################################################################

#CHAMADA 3
################################################################################################################################
# Construção do Payload (data)
soup = BeautifulSoup(response.text, 'html.parser')

path = soup.find("input", {"name": "path"})["value"]
partID = soup.find("input", {"name": "partID"})["value"]
authVersion = soup.find("input", {"name": "authVersion"})["value"]
csrf_token = soup.find("input", {"name": "_csrf"})["value"]
username = "249428520"
password = "SFHi3242v3"

data = f'path={path}&partID={partID}&authVersion={authVersion}&_csrf={csrf_token}&selectedAuthMethod=N&username={username}&password={password}'

headers.update({
    'Content-Type': 'application/x-www-form-urlencoded',
    'Origin': 'https://www.acesso.gov.pt',
    'Referer': 'https://www.acesso.gov.pt/v2/loginForm?partID=PFAP&path=/geral/dashboard',
    'Sec-Fetch-Site': 'same-site',
})

response = session.post('https://www.acesso.gov.pt/v2/login', cookies=session.cookies, data=data, headers=headers)
################################################################################################################################
#CHAMADA 4
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
    'Referer': 'https://www.acesso.gov.pt',
    'Sec-Fetch-Site': 'cross-site',
})

cookie_value = session.cookies['autentica_JSessionID']  # Isso já é o valor da cookie
# Salvar a cookie num ficheiro
with open('autentica_JSessionID.pkl', 'wb') as f:
    pickle.dump(cookie_value, f)

session.cookies.clear() # Não são necessárias as cookies desta resposta

response = session.post('https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard', data=data, headers=headers, allow_redirects=False)
################################################################################################################################
#CHAMADA 5
################################################################################################################################
response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/geral/', cookies=session.cookies, headers=headers, allow_redirects=False)
################################################################################################################################
#CHAMADA 6
################################################################################################################################
response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/geral/home', cookies=session.cookies, headers=headers)
################################################################################################################################

# Verificar se a resposta foi bem-sucedida (status 200)
if response.status_code == 200:
    with open('session.pkl', 'wb') as f:
        pickle.dump(session, f)
    print("Login realizado com sucesso e sessão salva.")
else:
    print("Erro ao comunicar.")

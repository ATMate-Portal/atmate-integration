import pickle
import requests
from bs4 import BeautifulSoup
import os

try:
    nif = os.environ.get("NIF")
    if not nif:
        nif = "248102818"
    password = os.environ.get("PASSWORD")
    if not password:
        password = "vtbt75D8_2024J#"
    scriptPath = "D:/Coding/IPT/ATMate/atmate-integration/atmateIntegration/src/main/resources/scripts/"
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

    # CHAMADA 3
    soup = BeautifulSoup(response.text, 'html.parser')

    try:
        path = soup.find("input", {"name": "path"})["value"]
        partID = soup.find("input", {"name": "partID"})["value"]
        authVersion = soup.find("input", {"name": "authVersion"})["value"]
        csrf_token = soup.find("input", {"name": "_csrf"})["value"]
    except TypeError:
        raise ValueError("Erro ao obter valores do formulário. O HTML pode ter mudado.")

    data = f'path={path}&partID={partID}&authVersion={authVersion}&_csrf={csrf_token}&selectedAuthMethod=N&username={nif}&password={password}'

    headers.update({
        'Content-Type': 'application/x-www-form-urlencoded',
        'Origin': 'https://www.acesso.gov.pt',
        'Referer': 'https://www.acesso.gov.pt/v2/loginForm?partID=PFAP&path=/geral/dashboard',
    })

    response = session.post('https://www.acesso.gov.pt/v2/login', data=data, headers=headers)

    # CHAMADA 4
    soup = BeautifulSoup(response.text, 'html.parser')

    try:
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
    except TypeError:
        raise ValueError("Erro ao obter dados do formulário após o login.")

    headers.update({
        'Referer': 'https://www.acesso.gov.pt',
        'Sec-Fetch-Site': 'cross-site',
    })

    cookie_value = session.cookies.get('autentica_JSessionID', None)
    if not cookie_value:
        raise ValueError("Erro: Cookie 'autentica_JSessionID' não foi encontrada.")

    with open(jsessionid_file_path, 'wb') as f:
        pickle.dump(cookie_value, f)

    session.cookies.clear()

    response = session.post('https://sitfiscal.portaldasfinancas.gov.pt/geral/dashboard', data=data, headers=headers, allow_redirects=False)

    # CHAMADA FINAL
    response = session.get('https://sitfiscal.portaldasfinancas.gov.pt/geral/home', cookies=session.cookies, headers=headers)

    if response.status_code == 200:
        try:
            with open(session_file_path, 'wb') as f:
                pickle.dump(session, f)
            print("Sessão salva com sucesso.")

        except Exception as e:
            print(f"Erro ao salvar a sessão: {e}")
    else:
        print("Erro ao comunicar com o sistema.")

except Exception as e:
    print(f"Ocorreu um erro inesperado: {e}")

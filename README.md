# 🧠 ATMate Integration

O `atmate-integration` é um serviço **crucial** no ecossistema ATMate, responsável por:

- Automatizar a recolha de dados da [Autoridade Tributária (AT) de Portugal](https://www.portaldasfinancas.gov.pt/).
- Persistir esses dados numa base de dados local.
- Gerir o envio de notificações via **SMS** e **Email** com base nas informações recolhidas.

Este serviço atua como o **motor de dados da aplicação**, garantindo que o `atmate-gateway` e o **frontend** tenham acesso a informações atualizadas e centralizadas.

---

## ✨ Funcionalidades Chave

### 🔎 Web Scraping Automatizado

- Execução **diária** de scripts Python.
- Acesso com **NIF e credenciais** dos clientes.
- Dados recolhidos:
  - **IMI** (Imposto Municipal sobre Imóveis)
  - **IUC** (Imposto Único de Circulação)
  - Dados pessoais, **moradas** e **contactos**
- ⚠️ Apenas leitura de dados (sem ações transacionais).

### 📀 Persistência de Dados

- Armazenamento **seguro** na base de dados.

### 📣 Gestão de Notificações

- Envio automático de alertas por **SMS** e **Email**.
- Ex: Notificações de prazos de pagamento de IUC.

### 🔗 Integração Java-Python

- Comunicação fluida entre o backend **Spring Boot** e scripts Python de scraping.

---

## 🚀 Tecnologias Utilizadas

### Backend (Java)

- `Spring Boot`, `Spring Data JPA`, `Lombok`, `SLF4J`

### Web Scraping (Python)

- `Requests`, `BeautifulSoup`

### Base de Dados

- `MySQL` 

### Outros

- `Maven` 
- `Swagger` (via SpringDoc OpenAPI)

---

## 📦 Estrutura do Projeto

```bash
atmate-integration/
├── src/main/java/com/atmate/portal/integration/atmateintegration/
│   ├── controller/         # Controladores REST
│   ├── database/
│   │   ├── entities/       # Entidades JPA (Client, Tax, etc.)
│   │   ├── repos/          # Repositórios Spring Data
│   │   └── services/       # Serviços de BD
│   ├── services/           # Scraping, notificações, envio de emails/SMS
│   ├── utils/              # Utilitários, enums, exceções
│   ├── dto/                # Objetos de transferência de dados 
│   ├── threads/            # Lógica multi-thread de invocação a web-scraping
│   ├── config/             # Utilitários, enums, exceções
│   └── AtmateIntegrationApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── scripts/            # Scripts Python de scraping
├── src/test/java/          # Testes unitários e de integração
├── pom.xml                 # Configuração Maven
└── README.md               # Este ficheiro
```

---

## 📄 Documentação da API

- Aceder à documentação gerada automaticamente: [localhost:8080/atmate-integration/swagger-ui/index.html](localhost:8080/atmate-integration/swagger-ui/index.html)

---

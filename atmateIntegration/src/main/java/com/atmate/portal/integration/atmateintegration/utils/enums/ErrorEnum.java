package com.atmate.portal.integration.atmateintegration.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorEnum {

    // Errors DATA
    TAX_NOT_FOUND("DATA-001", "Não foi encontrado nenhum imposto"),
    DATABASE_ERROR("DATA-002", "Erro ao aceder a base de dados"),
    INVALID_JSON("DATA-003", "Erro ao processar dados do imposto. Estrutura inválida."),
    INVALID_JSON_STRUCTURE("DATA-004", "Erro ao processar dados do imposto. Valores vazios."),
    INVALID_TAX_DATA("DATA-005", "Erro ao processar dados do imposto. Contém campos inválidos."),
    INVALID_TAX_DEADLINE_DATE("DATA-006", "Erro ao processar dados do imposto. Data Limite Pagamento inválida."),
    INVALID_TAX_CLIENT("DATA-007", "Erro ao processar dados do imposto. O cliente associado é inválido."),

    //INVALID DATA
    INVALID_NIF("INVALID-001", "NIF inválido. Deve conter exatamente 9 dígitos."),
    INVALID_PASSWORD("INVALID-002", "Password inválida. Deve ter pelo menos 6 caracteres."),

    //CREDENTIALS ERRORS
    CREDENTIAL_ERROR("CRED-001", "Erro ao guardar credenciais."),

    //CLIENT ERRORS
    CLIENT_ALREADY_EXISTS("CLIENT-001", "Já existe um cliente com este NIF."),
    CLIENT_SAVE_ERROR("CLIENT-002", "Erro ao guardar cliente."),
    CLIENT_NOT_FOUND("CLIENT-003", "O cliente não existe."),

    //SCRAPING ERROS
    SCRAPING_PERSONAL_DATA_ERROR("SCRAPING-001", "Erro scraping a obter dados do cliente"),
    SCRAPING_PYTHON_ERROR("SCRAPING-002", "Erro de código em python"),

    // AUTH ERROS
    AUTHENTICATION_FAILED("AUTH001", "Falha na autenticação."),
    UNAUTHORIZED("AUTH002", "Não autorizado."),


    // Adicione mais códigos de erro conforme necessário
    GENERIC_ERROR("GENERIC-001", "Erro Interno no Servidor");
    private final String errorCode;
    private final String message;
}
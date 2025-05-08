package com.atmate.portal.integration.atmateintegration.services;

import com.atmate.portal.integration.atmateintegration.utils.interfaces.SmsSendingInterface;

// Imports para HTTP e JSON
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode; // Para construir o corpo JSON

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections; // Para Collections.singletonList no corpo JSON

@Slf4j
@Service("SmsSender")
public class SmsSendingService implements SmsSendingInterface {

    @Value("${clicksend.username}")
    private String clicksendUsername;

    @Value("${clicksend.api.key}")
    private String clicksendApiKey;

    @Value("${clicksend.from.sms:#{null}}")
    private String clicksendFromPhoneNumber;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private boolean initialized = false;

    private static final String CLICKSEND_API_URL = "https://rest.clicksend.com/v3/sms/send";

    @PostConstruct
    public void init() { // Renomeado para evitar confusão com o nome do SDK
        if (!StringUtils.hasText(clicksendUsername)) {
            log.error("Username do ClickSend não configurado ou é um valor de exemplo. Por favor, defina 'clicksend.username' em application.properties.");
            return;
        }
        if (!StringUtils.hasText(clicksendApiKey)) {
            log.error("API Key do ClickSend não configurada ou é um valor de exemplo. Por favor, defina 'clicksend.api.key' em application.properties.");
            return;
        }

        try {
            this.restTemplate = new RestTemplate();
            this.objectMapper = new ObjectMapper();
            initialized = true;
            log.info("SmsSendingService (ClickSend HTTP Direto) configurado com sucesso para o utilizador: {}", clicksendUsername);
        } catch (Exception e) {
            log.error("Falha ao configurar o SmsSendingService (ClickSend HTTP Direto). Erro: {}", e.getMessage(), e);
            initialized = false;
        }
    }

    private HttpHeaders createHeaders() {
        String auth = clicksendUsername + ":" + clicksendApiKey;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    @Override
    public boolean sendSms(String toPhoneNumber, String messageBody) {
        if (!initialized) {
            log.error("SmsSendingService (ClickSend HTTP) não foi inicializado. Não é possível enviar SMS.");
            return false;
        }
        if (!StringUtils.hasText(toPhoneNumber)) {
            log.error("Número de telefone do destinatário não pode ser nulo ou vazio.");
            return false;
        }
        if (!StringUtils.hasText(messageBody)) {
            log.error("A mensagem do SMS não pode ser nula ou vazia.");
            return false;
        }

        String fullToPhoneNumber = toPhoneNumber.replaceAll("[^0-9+]", "");
        if (!fullToPhoneNumber.startsWith("+")) {
            if (fullToPhoneNumber.length() == 9) { // Número português típico
                fullToPhoneNumber = "+351" + fullToPhoneNumber;
            } else {
                log.warn("Número de telefone do destinatário '{}' não parece estar no formato internacional. A tentar enviar como está.", fullToPhoneNumber);
            }
        }

        // Construir o corpo JSON do pedido
        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("to", fullToPhoneNumber);
        messageNode.put("body", messageBody);
        if (StringUtils.hasText(clicksendFromPhoneNumber)) {
            messageNode.put("from", clicksendFromPhoneNumber);
        }
        // messageNode.put("source", "JavaAppSpring"); // Opcional
        // messageNode.put("custom_string", "seu-id-referencia"); // Opcional

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.set("messages", objectMapper.createArrayNode().add(messageNode));


        HttpEntity<String> requestEntity;
        try {
            requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), createHeaders());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Erro ao serializar o corpo do pedido JSON para ClickSend: {}", e.getMessage(), e);
            return false;
        }

        try {
            String fromDisplay = StringUtils.hasText(clicksendFromPhoneNumber) ? clicksendFromPhoneNumber : "Padrão ClickSend";
            log.info("A enviar SMS para: {} de: {} (ClickSend HTTP Direto)", fullToPhoneNumber, fromDisplay);
            log.debug("Corpo do pedido para ClickSend: {}", requestBody.toString());

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    CLICKSEND_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.debug("Resposta JSON crua do ClickSend (HTTP Direto): {}", responseEntity.getBody());

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
                String responseCodeApi = rootNode.path("response_code").asText();

                if ("SUCCESS".equalsIgnoreCase(responseCodeApi)) {
                    JsonNode messagesArray = rootNode.path("data").path("messages");
                    if (messagesArray.isArray() && !messagesArray.isEmpty()) {
                        JsonNode firstMessageResponse = messagesArray.get(0);
                        String messageStatus = firstMessageResponse.path("status").asText();
                        String messageId = firstMessageResponse.path("message_id").asText();

                        if ("SUCCESS".equalsIgnoreCase(messageStatus) || "QUEUED".equalsIgnoreCase(messageStatus)) {
                            log.info("SMS para {} enviado/enfileirado via ClickSend (HTTP Direto). Message ID: {}, Status: {}",
                                    fullToPhoneNumber, messageId, messageStatus);
                            return true;
                        } else {
                            log.warn("Falha ao enviar SMS para {} via ClickSend (HTTP Direto). Status da mensagem individual: {}. Message ID: {}. Resposta API: {}",
                                    fullToPhoneNumber, messageStatus, messageId, rootNode.path("response_msg").asText());
                            return false;
                        }
                    } else {
                        log.warn("SMS para {} via ClickSend (HTTP Direto): A chamada à API foi bem-sucedida (Código: {}), mas não foram retornados detalhes da mensagem. Mensagem API: {}",
                                fullToPhoneNumber, responseCodeApi, rootNode.path("response_msg").asText());
                        return false;
                    }
                } else {
                    log.error("Falha na API do ClickSend (HTTP Direto) ao enviar SMS para {}. Código Resposta API: {}, Mensagem Resposta: {}",
                            fullToPhoneNumber, responseCodeApi, rootNode.path("response_msg").asText());
                    return false;
                }
            } else {
                log.error("Erro HTTP ao enviar SMS para {} via ClickSend (HTTP Direto). Status: {}, Corpo: {}",
                        fullToPhoneNumber, responseEntity.getStatusCode(), responseEntity.getBody());
                return false;
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Erro HTTP (Client/Server) ao comunicar com ClickSend para {}: Status {}, Corpo da Resposta: {}",
                    fullToPhoneNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) { // Inclui JsonProcessingException do readTree
            log.error("Erro inesperado ao enviar SMS para {} via ClickSend (HTTP Direto): {}", fullToPhoneNumber, e.getMessage(), e);
            return false;
        }
    }
}

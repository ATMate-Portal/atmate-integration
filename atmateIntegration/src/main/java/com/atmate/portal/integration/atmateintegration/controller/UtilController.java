package com.atmate.portal.integration.atmateintegration.controller;

import com.atmate.portal.integration.atmateintegration.services.EmailSendingService;
import com.atmate.portal.integration.atmateintegration.services.NotificationService;
import com.atmate.portal.integration.atmateintegration.services.SmsSendingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/util")
public class UtilController {

    private static final Logger logger = LoggerFactory.getLogger(UtilController.class);

    @Autowired
    private SmsSendingService smsSendingService;

    @Autowired
    private EmailSendingService emailSendingService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check endpoint chamado.");
        return ResponseEntity.ok("A aplicação está a funcionar corretamente!");
    }

    /**
     * Endpoint de teste para simular o envio de um SMS.
     * @param phoneNumber O número de telefone para o qual enviar o SMS.
     * @param message O conteúdo da mensagem.
     * @return Uma resposta indicando o sucesso ou falha (simulado).
     */
    @PostMapping("/send-sms")
    public ResponseEntity<String> testSendSms(
            @RequestParam String phoneNumber,
            @RequestParam String message) {
        logger.info("Tentativa de envio de SMS de teste para: {} com mensagem: '{}'", phoneNumber, message);

        try {
             boolean smsSent = smsSendingService.sendSms(phoneNumber, message);
             if (smsSent) {
                 logger.info("SMS de teste enviado com sucesso para {}", phoneNumber);
                 return ResponseEntity.ok("SMS de teste enviado (simulado) para " + phoneNumber);
             } else {
                 logger.error("Falha ao enviar SMS de teste para {}", phoneNumber);
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao enviar SMS de teste (simulado)");
             }

        } catch (Exception e) {
            logger.error("Erro ao tentar enviar SMS de teste para {}: {}", phoneNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao tentar enviar SMS de teste (simulado): " + e.getMessage());
        }
    }

    /**
     * Endpoint de teste para simular o envio de um SMS.

     * @return Uma resposta indicando o sucesso ou falha (simulado).
     */
    @GetMapping("/runNotificationConfig")
    public void runNotificationConfig() throws JsonProcessingException {
        notificationService.prepareNotifications();
    }

    /**
     * Endpoint de teste para simular o envio de um Email.
     * @param toEmail O endereço de email do destinatário.
     * @param subject O assunto do email.
     * @param body O corpo do email.
     * @return Uma resposta indicando o sucesso ou falha (simulado).
     */
    @PostMapping("/send-email")
    public ResponseEntity<String> testSendEmail(
            @RequestParam String toEmail,
            @RequestParam String subject,
            @RequestParam String body) {
        logger.info("Tentativa de envio de Email de teste para: {} com assunto: '{}'", toEmail, subject);


        try {
            emailSendingService.sendEmail(toEmail, subject, body, true);
            logger.info("Email de teste enviado com sucesso para {}", toEmail);
            return ResponseEntity.ok("Email de teste enviado (simulado) para " + toEmail);

        } catch (Exception e) {
            logger.error("Erro ao tentar enviar email de teste para {}: {}", toEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao tentar enviar email de teste (simulado): " + e.getMessage());
        }
    }
}

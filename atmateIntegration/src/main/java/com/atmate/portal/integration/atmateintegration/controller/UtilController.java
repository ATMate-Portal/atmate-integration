package com.atmate.portal.integration.atmateintegration.controller;

import com.atmate.portal.integration.atmateintegration.services.EmailSendingService;
import com.atmate.portal.integration.atmateintegration.services.NotificationService;
import com.atmate.portal.integration.atmateintegration.services.SmsSendingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Utils e Testes", description = "Endpoints para testes de aplicação, envio de SMS e e-mails, e execução de configurações de notificação.")
public class UtilController {

    private static final Logger logger = LoggerFactory.getLogger(UtilController.class);

    @Autowired
    private SmsSendingService smsSendingService;

    @Autowired
    private EmailSendingService emailSendingService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/health")
    @Operation(
            summary = "Verificação o estado da Aplicação",
            description = "Verifica se a aplicação está em execução e a responder corretamente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "A aplicação está a funcionar corretamente.",
                    content = @Content(schema = @Schema(type = "string", example = "A aplicação está a funcionar corretamente!"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
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
    @Operation(
            summary = "Envio de SMS",
            description = "Envia um SMS para um número de telefone especificado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SMS enviado com sucesso.",
                    content = @Content(schema = @Schema(type = "string", example = "SMS enviado para 912345678"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Falha interna ao tentar enviar o SMS ou erro na comunicação com o serviço de SMS.",
                    content = @Content(schema = @Schema(type = "string", example = "Erro interno ao tentar enviar SMS: Erro na API do SMS"))
            )
    })
    public ResponseEntity<String> testSendSms(
            @Parameter(
                    description = "O número de telefone do destinatário do SMS (com código de país, ex: '351912345678').",
                    required = true,
                    example = "351912345678"
            )
            @RequestParam String phoneNumber,
            @Parameter(
                    description = "O conteúdo da mensagem SMS a ser enviada.",
                    required = true,
                    example = "Olá, este é um SMS de teste da aplicação!"
            )
            @RequestParam String message) {
        logger.info("Tentativa de envio de SMS para: {} com mensagem: '{}'", phoneNumber, message);

        try {
            boolean smsSent = smsSendingService.sendSms(phoneNumber, message);
            if (smsSent) {
                logger.info("SMS enviado com sucesso para {}", phoneNumber);
                return ResponseEntity.ok("SMS para " + phoneNumber);
            } else {
                logger.error("Falha ao enviar SMS para {}", phoneNumber);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao enviar SMS");
            }

        } catch (Exception e) {
            logger.error("Erro ao tentar enviar SMS para {}: {}", phoneNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao tentar enviar SMS: " + e.getMessage());
        }
    }

    @GetMapping("/runNotificationConfig")
    @Operation(
            summary = "Preparar Configuração de Notificações",
            description = "Invoca preparação das notificações baseadas nas configurações existentes na base de dados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Processamento das notificações iniciado com sucesso.",
                    content = @Content(schema = @Schema(implementation = Void.class)) // Retorna void
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor durante o processamento das notificações.",
                    content = @Content(schema = @Schema(implementation = String.class, example = "Erro interno ao processar notificações."))
            )
    })
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
    @Operation(
            summary = "Envio de Email",
            description = "Envia um e-mail para um endereço especificado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email enviado com sucesso (simulado).",
                    content = @Content(schema = @Schema(type = "string", example = "Email enviado para teste@example.com"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Falha interna ao tentar enviar o email ou erro na comunicação com o serviço de e-mail.",
                    content = @Content(schema = @Schema(type = "string", example = "Erro interno ao tentar enviar email: Erro na conexão SMTP"))
            )
    })
    public ResponseEntity<String> testSendEmail(
            @Parameter(
                    description = "O endereço de e-mail do destinatário.",
                    required = true,
                    example = "teste@example.com"
            )
            @RequestParam String toEmail,
            @Parameter(
                    description = "O assunto do e-mail.",
                    required = true,
                    example = "Assunto do Teste de Email"
            )
            @RequestParam String subject,
            @Parameter(
                    description = "O corpo do e-mail.",
                    required = true,
                    example = "Este é o corpo da mensagem de email."
            )
            @RequestParam String body) {
        logger.info("Tentativa de envio de Email para: {} com assunto: '{}'", toEmail, subject);


        try {
            emailSendingService.sendEmail(toEmail, subject, body, true);
            logger.info("Email enviado com sucesso para {}", toEmail);
            return ResponseEntity.ok("Email enviado (simulado) para " + toEmail);

        } catch (Exception e) {
            logger.error("Erro ao tentar enviar email para {}: {}", toEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao tentar enviar email: " + e.getMessage());
        }
    }
}

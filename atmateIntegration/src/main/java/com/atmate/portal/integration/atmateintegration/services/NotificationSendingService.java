package com.atmate.portal.integration.atmateintegration.services;


import com.atmate.portal.integration.atmateintegration.database.entitites.ClientNotification;
import com.atmate.portal.integration.atmateintegration.database.entitites.Contact;
import com.atmate.portal.integration.atmateintegration.database.entitites.ContactType; // Certifique-se que está correto
import com.atmate.portal.integration.atmateintegration.database.services.ClientNotificationService;
import com.atmate.portal.integration.atmateintegration.database.services.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Opcional, mas bom para atomicidade

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationSendingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSendingService.class);

    @Autowired
    private ClientNotificationService clientNotificationService; // Para buscar e atualizar notificações
    @Autowired
    private ContactService contactService;

    @Autowired
    private EmailSendingService emailSendingService; // Serviço de envio de email

    @Autowired
    private SmsSendingService smsSendingService; // Serviço de envio de SMS

    public void processAndSendPendingNotifications() {
        log.info("--- [NotificationSendingService] Iniciando ciclo de envio de notificações pendentes ---");

        List<ClientNotification> pendingNotifications = clientNotificationService.findPendingNotificationDueToday();

        if (pendingNotifications.isEmpty()) {
            log.info("[NotificationSendingService] Nenhuma notificação pendente encontrada para envio.");
            return;
        }

        log.info("[NotificationSendingService] {} notificações pendentes encontradas. Processando...", pendingNotifications.size());

        for (ClientNotification notification : pendingNotifications) {
            processSingleNotification(notification);
        }

        log.info("--- [NotificationSendingService] Ciclo de envio de notificações pendentes concluído ---");
    }

    @Transactional
    public void processSingleNotification(ClientNotification notification) {
        log.debug("[NotificationSendingService] Processando notificação ID: {}", notification.getId());
        boolean sentSuccessfully = false;
        String errorMessage = null;
        int retryCount = 0;

        try {
            ContactType contactType = notification.getNotificationType();
            if (contactType == null || contactType.getDescription() == null) {
                errorMessage = "Tipo de contacto não definido ou inválido.";
                log.warn("[NotificationSendingService] Notificação ID {}: {}", notification.getId(), errorMessage);
                updateNotificationStatus(notification, "FALHOU", LocalDateTime.now()); // 0 retries left
                return;
            }

            Contact contact = contactService.getContactByClientAndType(notification.getClient(), notification.getNotificationType());

            String channelDescription = contactType.getDescription();
            String recipientAddress = contact.getContact(); // Será preenchido abaixo

            if ("Email".equalsIgnoreCase(channelDescription)) {
                if (notification.getClient() != null) {

                    if(recipientAddress!=null){
                       sentSuccessfully = emailSendingService.sendEmail(
                                recipientAddress,
                                notification.getTitle(),
                                notification.getMessage(), true);
                    }else {
                        errorMessage = "Email do cliente em falta para envio por Email.";
                        log.warn("[NotificationSendingService] Notificação ID {}: {}", notification.getId(), errorMessage);
                    }
                } else {
                    errorMessage = "Dados do cliente em falta para envio por Email.";
                    log.warn("[NotificationSendingService] Notificação ID {}: {}", notification.getId(), errorMessage);
                }
            } else if ("Telefone".equalsIgnoreCase(channelDescription)) { // Assumindo que "Telefone" é para SMS
                if (notification.getClient() != null) { // Supondo que Client tem getPhoneNumber()

                    if(recipientAddress!=null){
                        sentSuccessfully  = smsSendingService.sendSms(
                                recipientAddress,
                                notification.getMessage() // A mensagem já deve ser curta e apropriada para SMS
                        );
                    }else{
                        errorMessage = "Telefone do cliente em falta para envio por SMS.";
                        log.warn("[NotificationSendingService] Notificação ID {}: {}", notification.getId(), errorMessage);
                    }

                } else {
                    errorMessage = "Dados do cliente em falta para envio por SMS.";
                    log.warn("[NotificationSendingService] Notificação ID {}: {}", notification.getId(), errorMessage);
                }
            } else {
                errorMessage = "Canal de notificação desconhecido: " + channelDescription;
                log.warn("[NotificationSendingService] Notificação ID {}: {}", notification.getId(), errorMessage);
            }

            if (sentSuccessfully) {
                log.info("[NotificationSendingService] Notificação ID {} enviada com sucesso para {} via {}.", notification.getId(), recipientAddress, channelDescription);
                updateNotificationStatus(notification, "ENVIADA", LocalDateTime.now()); // Mantém retryCount
            } else {
                // Se não foi bem sucedido e não houve erro antes (ex: dados em falta), é uma falha de envio
                if (errorMessage == null) errorMessage = "Falha no serviço de envio " + channelDescription;
                log.warn("[NotificationSendingService] Falha ao enviar notificação ID {} para {} via {}. Erro: {}", notification.getId(), recipientAddress, channelDescription, errorMessage);
                // Lógica de Retry Simples: Decrementa retryCount ou marca como falha se 0.
                ///int retriesLeft = handleRetry(notification);
                updateNotificationStatus(notification, "FALHOU", null);
            }

        } catch (Exception e) {
            log.error("[NotificationSendingService] Erro inesperado ao processar notificação ID: {}.", notification.getId(), e);
            //int retriesLeft = handleRetry(notification);
            updateNotificationStatus(notification, "FALHOU", null);
        }
    }

    private void updateNotificationStatus(ClientNotification notification, String status, LocalDateTime sendDate) {
        notification.setStatus(status);
        notification.setSendDate(sendDate); // Pode ser nulo se o estado for PENDENTE após retry
        try {
            clientNotificationService.updateClientNotification(notification.getId(), notification); // Supondo que tem este método
        } catch (Exception e) {
            log.error("[NotificationSendingService] CRÍTICO: Falha ao atualizar o estado da notificação ID {} para {}", notification.getId(), status, e);
        }
    }

}
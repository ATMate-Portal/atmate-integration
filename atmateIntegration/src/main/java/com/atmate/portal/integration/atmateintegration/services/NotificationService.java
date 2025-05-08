package com.atmate.portal.integration.atmateintegration.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.*;
import com.atmate.portal.integration.atmateintegration.database.services.ClientNotificationConfigService;
import com.atmate.portal.integration.atmateintegration.database.services.ClientNotificationService;
import com.atmate.portal.integration.atmateintegration.database.services.TaxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    ClientNotificationService clientNotificationService;
    @Autowired
    ClientNotificationConfigService clientNotificationConfigService;
    @Autowired
    TaxService taxService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    NotificationSendingService notificationSendingService;

    private static final String DEFAULT_NOTIFICATION_TITLE = "Lembrete Fiscal ATMate";
    private static final String DEFAULT_NOTIFICATION_MESSAGE = "Verifique os seus prazos fiscais.";

    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Lisbon")
    public void prepareNotifications() throws JsonProcessingException { // Removido "throws Exception" para tratamento interno
        LocalDate today = LocalDate.now();
        List<ClientNotificationConfig> clientNotificationConfigList;

        try {
            clientNotificationConfigList = clientNotificationConfigService.getAllActiveClientNotificationConfigs(true);
        } catch (Exception e) {
            log.error("Erro ao buscar configurações de notificação de cliente: {}", e.getMessage(), e);
            return; // Sai se não conseguir buscar as configurações
        }

        if (clientNotificationConfigList.isEmpty()) {
            log.info("Nenhuma configuração de notificação de cliente ativa encontrada em {}.", today);
            return;
        }

        log.info("Iniciando tarefa prepareNotifications para {}. Encontradas {} configurações ativas.", today, clientNotificationConfigList.size());

        for (ClientNotificationConfig config : clientNotificationConfigList) {
            Client client = config.getClient();
            TaxType taxType = config.getTaxType();
            ContactType contactTypeForNotification = config.getNotificationType(); // O canal de notificação (Email, SMS)
            String frequency = config.getFrequency();
            long numericStartPeriod;

            try {
                numericStartPeriod = Long.parseLong(String.valueOf(config.getStartPeriod()));
                if (numericStartPeriod <= 0) {
                    log.warn("startPeriod inválido (deve ser > 0): {} para config ID {}. A ignorar esta configuração.", config.getStartPeriod(), config.getId());
                    continue;
                }
            } catch (NumberFormatException e) {
                log.warn("Formato de startPeriod inválido: {} para config ID {}. A ignorar esta configuração.", config.getStartPeriod(), config.getId());
                continue;
            }

            List<Tax> clientTaxList;
            try {
                clientTaxList = taxService.getTaxesByClientAndType(client, taxType);
            } catch (Exception e) {
                log.error("Erro ao buscar impostos para cliente {} e tipo de imposto {}: {}", client.getId(), taxType.getDescription(), e.getMessage(), e);
                continue; // Próxima configuração
            }

            if (clientTaxList.isEmpty()) {
                log.debug("Nenhum imposto encontrado para cliente {} e tipo de imposto {} (config ID {}).", client.getId(), taxType.getDescription(), config.getId());
                continue;
            }

            for (Tax clientTax : clientTaxList) {
                LocalDate paymentDeadline = clientTax.getPaymentDeadline();
                if (paymentDeadline == null) {
                    log.warn("Imposto ID {} para cliente {} tem paymentDeadline nula. A ignorar.", clientTax.getId(), client.getId());
                    continue;
                }

                boolean hasPaymentDateDue = paymentDeadline.isBefore(today);

                // Se o prazo de pagamento já passou, não há necessidade de notificar
                if (hasPaymentDateDue) {
                    log.debug("Prazo de pagamento {} para imposto ID {} já passou. A ignorar.", paymentDeadline, clientTax.getId());
                    continue;
                }

                // Itera de 'numericStartPeriod' (ex: 4 semanas antes) até '1' (ex: 1 semana antes)
                for (long periodInstance = numericStartPeriod; periodInstance >= 1; periodInstance--) {
                    LocalDate potentialNotificationDate;
                    try {
                        potentialNotificationDate = calculateSpecificNotificationDate(paymentDeadline, frequency, periodInstance);
                    } catch (IllegalArgumentException e) {
                        log.warn("Não foi possível calcular potentialNotificationDate para config ID {}, imposto ID {}: {}. A ignorar este cálculo.", config.getId(), clientTax.getId(), e.getMessage());
                        continue; // Próxima iteração de periodInstance
                    }

                    potentialNotificationDate = LocalDate.of(2025, 5, 8);

                    if (potentialNotificationDate.isEqual(today)) {
                        log.info("Coincidência encontrada: Preparar notificação para cliente {}, tipo imposto {}, imposto ID {}, prazo pagº {}, data notif.: {}. (Regra: {} {} antes do prazo)",
                                client.getId(), taxType.getDescription(), clientTax.getId(), paymentDeadline, today, periodInstance, frequency);

                        // OPCIONAL: Verificar se já existe uma notificação para este cliente, imposto e data.
                        /* boolean alreadyExists = clientNotificationService.existsForClientTaxAndDate(client, taxType, today);
                         if (alreadyExists) {
                            log.info("Notificação para cliente {}, imposto ID {} em {} já existe. A ignorar criação.", client.getId(), clientTax.getId(), today);
                           break;
                         }*/

                        ClientNotification clientNotification = new ClientNotification();
                        clientNotification.setClient(client);
                        clientNotification.setTaxType(taxType); // Associar ao imposto específico
                        clientNotification.setClientNotificationConfig(config); // Associar à configuração que originou
                        clientNotification.setNotificationType(contactTypeForNotification); // Canal de envio (Email, SMS)
                        clientNotification.setCreateDate(today); // Data em que a notificação é determinada
                        clientNotification.setSendDate(null); // Data de envio real será definida posteriormente
                        clientNotification.setStatus("PENDENTE");

                        String notificationTitle = getNotificationTitle(contactTypeForNotification, client, taxType, clientTax, paymentDeadline);
                        String notificationMessage = getNotificationMessage(contactTypeForNotification, client, taxType, clientTax, paymentDeadline);

                        clientNotification.setTitle(notificationTitle);
                        clientNotification.setMessage(notificationMessage);

                        try {
                            clientNotificationService.createClientNotification(clientNotification);
                            log.info("Registo ClientNotification ID {} criado com sucesso para cliente {}, imposto ID {}.", clientNotification.getId(), client.getId(), clientTax.getId());
                        } catch (Exception e) {
                            log.error("Falha ao guardar ClientNotification para cliente {}, imposto ID {}: {}", client.getId(), clientTax.getId(), e.getMessage(), e);
                            // Considerar como tratar esta falha
                        }
                        // Se uma notificação foi criada hoje para este imposto/config, não precisamos verificar outros periodInstance para o mesmo.
                        break; // Sair do loop de periodInstance
                    }
                } // fim loop periodInstance
            } // fim loop clientTaxList
        } // fim loop clientNotificationConfigList
        log.info("Tarefa prepareNotifications concluída para {}.", today);
        notificationSendingService.processAndSendPendingNotifications();
    }

    /**
     * Calcula uma data de notificação específica com base no prazo de pagamento, frequência e
     * qual instância de período (ex: a 1ª semana antes, 2ª semana antes).
     * @param paymentDeadline A data limite de pagamento do imposto.
     * @param frequency A frequência da notificação ("Diário", "Semanal", "Mensal", "Trimestral").
     * @param periodInstance Qual ocorrência do período (ex: 1 para 1 semana antes, 2 para 2 semanas antes).
     * @return A data calculada para a notificação.
     * @throws IllegalArgumentException Se a frequência for desconhecida ou periodInstance não for positivo.
     */
    private LocalDate calculateSpecificNotificationDate(LocalDate paymentDeadline, String frequency, long periodInstance) throws IllegalArgumentException {
        if (periodInstance <= 0) {
            throw new IllegalArgumentException("periodInstance tem de ser positivo.");
        }
        return switch (frequency) {
            case "Diário" -> paymentDeadline.minusDays(periodInstance);
            case "Semanal" -> paymentDeadline.minusWeeks(periodInstance);
            case "Mensal" -> paymentDeadline.minusMonths(periodInstance);
            case "Trimestral" -> paymentDeadline.minusMonths(periodInstance * 3);
            default -> {
                log.warn("Tipo de frequência desconhecido: {}", frequency);
                throw new IllegalArgumentException("Tipo de frequência desconhecido: " + frequency);
            }
        };
    }

    // Métodos para gerar título e mensagem - agora recebem mais contexto
    private String getNotificationTitle(ContactType contactType, Client client, TaxType taxType, Tax tax, LocalDate paymentDeadline) {
        String baseTitle;
        // Assumindo que ContactType tem getDescription() para "Email", "Telefone"
        // Se ContactType for um Enum, pode usar diretamente no switch.
        switch (contactType.getDescription()) {
            case "Telefone":
                baseTitle = "Lembrete SMS ATMate:";
                break;
            case "Email":
                baseTitle = "Lembrete Email ATMate:";
                break;
            default:
                log.warn("Descrição de ContactType desconhecida para título: {}", contactType.getDescription());
                baseTitle = DEFAULT_NOTIFICATION_TITLE; // Fallback
        }
        // Supondo que TaxType tem getDescription() e LocalDate.toString() é adequado.
        return String.format("%s %s - Prazo: %s", baseTitle, taxType.getDescription(), paymentDeadline.toString());
    }

    private String getNotificationMessage(ContactType contactType, Client client, TaxType taxType, Tax tax, LocalDate paymentDeadline) throws JsonProcessingException {
        String clientName = client.getName(); // Supondo que Client tem getName()
        String taxDescription = taxType.getDescription();

        JsonNode jsonNode = objectMapper.readTree(tax.getTaxData());;
        String taxReference = tax.getIdentifier(jsonNode);
        String deadlineStr = paymentDeadline.toString();

        String detailedMessage = String.format(
                "Caro(a) %s,\n\nEste é um lembrete sobre o seu imposto '%s' (Ref: %s) com data limite de pagamento a %s.\n\nAtentamente,\nA Equipa ATMate",
                clientName,
                taxDescription,
                taxReference,
                deadlineStr
        );

        // Mensagens SMS devem ser mais curtas
        // Supondo que TaxType tem getShortDescription() ou algo similar
        // Fallback
        return switch (contactType.getDescription()) {
            case "Telefone" -> {
                String shortTaxDesc = taxType.getDescription() != null ? taxType.getDescription() : taxDescription;
                yield String.format("Lembrete ATMate: Imposto %s, prazo %s. Ref: %s.", shortTaxDesc, deadlineStr, taxReference);
            }
            case "Email" -> detailedMessage;
            default -> {
                log.warn("Descrição de ContactType desconhecida para mensagem: {}", contactType.getDescription());
                yield DEFAULT_NOTIFICATION_MESSAGE;
            }
        };
    }

}
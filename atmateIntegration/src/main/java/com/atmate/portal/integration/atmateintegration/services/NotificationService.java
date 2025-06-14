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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            log.info("Erro ao buscar configurações de notificação de cliente: {}", e.getMessage(), e);
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
                    log.info("startPeriod inválido (deve ser > 0): {} para config ID {}. A ignorar esta configuração.", config.getStartPeriod(), config.getId());
                    continue;
                }
            } catch (NumberFormatException e) {
                log.info("Formato de startPeriod inválido: {} para config ID {}. A ignorar esta configuração.", config.getStartPeriod(), config.getId());
                continue;
            }

            List<Tax> clientTaxList;
            try {
                clientTaxList = taxService.getTaxesByClientAndType(client, taxType);
            } catch (Exception e) {
                log.info("Erro ao buscar impostos para cliente {} e tipo de imposto {}: {}", client.getId(), taxType.getDescription(), e.getMessage(), e);
                continue; // Próxima configuração
            }

            if (clientTaxList.isEmpty()) {
                log.info("Nenhum imposto encontrado para cliente {} e tipo de imposto {} (config ID {}).", client.getId(), taxType.getDescription(), config.getId());
                continue;
            }

            for (Tax clientTax : clientTaxList) {
                LocalDate paymentDeadline = clientTax.getPaymentDeadline();
                if (paymentDeadline == null) {
                    log.info("Imposto ID {} para cliente {} tem paymentDeadline nula. A ignorar.", clientTax.getId(), client.getId());
                    continue;
                }

                boolean hasPaymentDateDue = paymentDeadline.isBefore(today);

                // Se o prazo de pagamento já passou, não há necessidade de notificar
                if (hasPaymentDateDue) {
                    log.info("Prazo de pagamento {} para imposto ID {} já passou. A ignorar.", paymentDeadline, clientTax.getId());
                    continue;
                }

                // Itera de 'numericStartPeriod' (ex: 4 semanas antes) até '1' (ex: 1 semana antes)
                for (long periodInstance = numericStartPeriod; periodInstance >= 1; periodInstance--) {
                    LocalDate potentialNotificationDate;
                    try {
                        potentialNotificationDate = calculateSpecificNotificationDate(paymentDeadline, frequency, periodInstance);
                    } catch (IllegalArgumentException e) {
                        log.info("Não foi possível calcular potentialNotificationDate para config ID {}, imposto ID {}: {}. A ignorar este cálculo.", config.getId(), clientTax.getId(), e.getMessage());
                        continue; // Próxima iteração de periodInstance
                    }

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
                            log.info("Falha ao guardar ClientNotification para cliente {}, imposto ID {}: {}", client.getId(), clientTax.getId(), e.getMessage(), e);
                            // Considerar como tratar esta falha
                        }
                        // Se uma notificação foi criada hoje para este imposto/config, não precisamos verificar outros periodInstance para o mesmo.
                        break; // Sair do loop de periodInstance
                    }else if(potentialNotificationDate.isBefore(today)){
                        break;
                    }
                } // fim loop periodInstance
            } // fim loop clientTaxList
        } // fim loop clientNotificationConfigList
        log.info("Tarefa prepareNotifications concluída para {}.", today);
        notificationSendingService.processAndSendPendingNotifications();
    }

    /**
     * Prepara e despoleta o envio de notificações para uma única ClientNotificationConfig.
     * Este método é ideal para a funcionalidade "Forçar Envio".
     *
     * @param configId O ID da ClientNotificationConfig para a qual forçar o envio.
     * @return O número de notificações que foram criadas e para as quais o envio foi tentado.
     * @throws JsonProcessingException Se houver um erro ao processar dados JSON para a mensagem.
     * @throws IllegalStateException Se a configuração estiver num estado que não permite o envio forçado (ex: inativa, se essa for uma regra de negócio).
     */
    public int prepareAndTriggerSingleConfigNotifications(Integer configId) throws Exception, JsonProcessingException, IllegalStateException {
        log.info("Iniciando preparação e envio forçado para config ID: {}", configId);
        int notificationsCreatedAndTriggeredCount = 0;

        Optional<ClientNotificationConfig> configOpt = clientNotificationConfigService.getClientNotificationConfigById(configId);

        if (configOpt.isEmpty()) {
            log.info("Configuração de notificação com ID {} não encontrada para envio forçado.", configId);
            throw new Exception("Configuração de notificação com ID " + configId + " não encontrada.");
        }

        ClientNotificationConfig config = configOpt.get();


        Client clientFromConfig = config.getClient(); // Pode ser null para configurações globais
        TaxType taxType = config.getTaxType();
        ContactType contactTypeForNotification = config.getNotificationType();

        if (taxType == null || contactTypeForNotification == null) {
            log.info("Configuração ID {} tem taxType ou notificationType nulos. Não é possível preparar a notificação.", configId);
            return 0; // Ou lançar uma exceção
        }

        List<Client> clientsToProcess = new ArrayList<>();
        if (clientFromConfig != null) {
            // Se a configuração é para um cliente específico, processa apenas para ele.
            clientsToProcess.add(clientFromConfig);
            log.info("Configuração ID {} é para cliente específico ID: {}.", configId, clientFromConfig.getId());
        }

        LocalDate today = LocalDate.now(); // Usado para data de criação da notificação

        for (Client currentClient : clientsToProcess) {
            List<Tax> clientTaxList;
            try {
                clientTaxList = taxService.getTaxesByClientAndType(currentClient, taxType);
            } catch (Exception e) {
                log.info("Erro ao obter impostos para cliente ID {} e tipo de imposto {}: {}", currentClient.getId(), taxType.getDescription(), e.getMessage(), e);
                continue; // Próximo cliente
            }

            if (clientTaxList.isEmpty()) {
                log.info("Nenhum imposto do tipo '{}' encontrado para cliente ID {} (relevante para config ID {}).", taxType.getDescription(), currentClient.getId(), config.getId());
                continue;
            }

            for (Tax clientTax : clientTaxList) {
                LocalDate paymentDeadline = clientTax.getPaymentDeadline();
                if (paymentDeadline == null) {
                    log.info("Imposto ID {} para cliente ID {} tem paymentDeadline nula. A ignorar.", clientTax.getId(), currentClient.getId());
                    continue;
                }

                // Para "Forçar Envio", não verificamos a data com base na frequência/startPeriod.
                // Apenas verificamos se o prazo ainda não passou.
                if (paymentDeadline.isBefore(today)) {
                    log.info("Prazo de pagamento {} para imposto ID {} (cliente ID {}) já passou.", paymentDeadline, clientTax.getId(), currentClient.getId());
                    //continue;
                }

                // Lógica para criar e guardar a ClientNotification
                ClientNotification clientNotification = new ClientNotification();
                clientNotification.setClient(currentClient);
                clientNotification.setTaxType(taxType);
                clientNotification.setClientNotificationConfig(config); // Associar à config que originou
                clientNotification.setNotificationType(contactTypeForNotification);
                clientNotification.setCreateDate(today);
                clientNotification.setSendDate(null); // Será preenchido pelo NotificationSendingService
                clientNotification.setStatus("PENDENTE"); // Ou um status como "PENDENTE_FORCED"

                String notificationTitle = getNotificationTitle(contactTypeForNotification, currentClient, taxType, clientTax, paymentDeadline);
                String notificationMessage = getNotificationMessage(contactTypeForNotification, currentClient, taxType, clientTax, paymentDeadline);

                clientNotification.setTitle(notificationTitle);
                clientNotification.setMessage(notificationMessage);

                try {
                    clientNotificationService.createClientNotification(clientNotification);
                    log.info("Envio Forçado: ClientNotification ID {} criada para cliente ID {}, imposto ID {}.", clientNotification.getId(), currentClient.getId(), clientTax.getId());
                    notificationsCreatedAndTriggeredCount++;
                } catch (Exception e) {
                    log.info("Envio Forçado: Falha ao guardar ClientNotification para cliente ID {}, imposto ID {}: {}", currentClient.getId(), clientTax.getId(), e.getMessage(), e);
                    // Considerar se deve continuar ou parar em caso de erro aqui
                }
            } // fim loop clientTaxList
        } // fim loop clientsToProcess

        if (notificationsCreatedAndTriggeredCount > 0) {
            log.info("Total de {} notificações preparadas para envio forçado para config ID {}. A despoletar o serviço de envio.", notificationsCreatedAndTriggeredCount, configId);
            notificationSendingService.processAndSendPendingNotifications(); // Tenta enviar imediatamente
        } else {
            log.info("Nenhuma notificação foi preparada para envio forçado para config ID {} (ex: prazos já passaram, sem impostos correspondentes).", configId);
        }

        log.info("Processo de preparação e envio forçado para config ID {} concluído.", configId);
        return notificationsCreatedAndTriggeredCount;
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
                log.info("Tipo de frequência desconhecido: {}", frequency);
                throw new IllegalArgumentException("Tipo de frequência desconhecido: " + frequency);
            }
        };
    }

    // Métodos para gerar título e mensagem - agora recebem mais contexto
    private String getNotificationTitle(ContactType contactType, Client client, TaxType taxType, Tax tax, LocalDate paymentDeadline) {
        // Fallback
        String baseTitle = switch (contactType.getDescription()) {
            case "Telefone" -> "Lembrete SMS ATMate:";
            case "Email" -> "Lembrete Email ATMate:";
            default -> {
                log.info("Descrição de ContactType desconhecida para título: {}", contactType.getDescription());
                yield DEFAULT_NOTIFICATION_TITLE;
            }
        };
        return String.format("%s %s - Prazo: %s", baseTitle, taxType.getDescription(), paymentDeadline.toString());
    }

    private String getNotificationMessage(ContactType contactType, Client client, TaxType taxType, Tax tax, LocalDate paymentDeadline) throws JsonProcessingException {
        String clientName = client.getName(); // Supondo que Client tem getName()
        String taxDescription = taxType.getDescription();

        JsonNode jsonNode = objectMapper.readTree(tax.getTaxData());;
        String taxReference = tax.getIdentifier(jsonNode);
        String deadlineStr = paymentDeadline.toString();


        return switch (contactType.getDescription()) {
            case "Telefone" -> {
                String usedTaxDesc = taxType.getDescription() != null ? taxType.getDescription() : taxDescription;

                yield String.format("ATMate Lembrete: %s prazo %s. Ref: %s.", usedTaxDesc, deadlineStr, taxReference);

                // Versão alternativa com ênfase na ação/benefício
                // yield String.format("ATMate: Pagar %s ate %s. Ref: %s. Evite coimas.", usedTaxDesc, deadlineStr, taxReference);
            }
            case "Email" -> {
                String subject = String.format("Lembrete ATMate: Pagamento de %s", taxDescription);

                // Construir o corpo HTML com inline CSS para melhor compatibilidade

                yield String.format("""
                            <!DOCTYPE html>
                            <html lang="pt">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>%s</title>
                                <style>
                                    body { font-family: sans-serif; line-height: 1.6; color: #333; }
                                    .container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; background-color: #f9f9f9; }
                                    .header { text-align: center; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 1px solid #eee; }
                                    .header h1 { color: #0056b3; /* Cor da ATMate? */ margin: 0; }
                                    .content h2 { color: #333; }
                                    .details { margin: 20px 0; padding: 15px; background-color: #fff; border: 1px solid #eee; border-radius: 4px; }
                                    .details p { margin: 5px 0; }
                                    .details strong { color: #0056b3; /* Cor da ATMate? */ }
                                    .actions { margin-top: 25px; text-align: center; }
                                    .actions a { display: inline-block; padding: 10px 15px; background-color: #007bff; /* Cor do botão */ color: #fff; text-decoration: none; border-radius: 4px; }
                                    .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 0.9em; color: #777; text-align: center; }
                                    .footer p { margin: 5px 0; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h1>ATMate</h1>
                                        <p>O seu assistente de obrigações fiscais</p>
                                    </div>
                                    <div class="content">
                                        <h2>Lembrete de Pagamento de Imposto</h2>
                                        <p>Estimado(a) %s,</p>
                                        <p>Este é um lembrete automático sobre o prazo de pagamento de uma das suas obrigações fiscais:</p>
                                        <div class="details">
                                            <p><strong>Imposto:</strong> %s</p>
                                            <p><strong>Referência:</strong> %s</p>
                                            <p><strong>Data Limite de Pagamento:</strong> <strong style="color:#dc3545;">%s</strong></p>
                                        </div>
                                        <p>Recomendamos que efetue o pagamento atempadamente para evitar coimas ou juros de mora. Pode geralmente efetuar o pagamento através do Portal das Finanças, Multibanco ou Home Banking.</p>
                                     </div>
                                     <div class="actions">
                                         </div>
                                     <div class="footer">
                                        <p>Este lembrete foi enviado com base nas suas configurações na plataforma ATMate.</p>
                                        <p>Precisa de ajuda? Consulte a nossa <a href="%s" target="_blank">Ajuda</a> ou contacte-nos via <a href="mailto:%s">%s</a>.</p>
                                        <p><a href="%s" target="_blank">Gerir preferências de notificação</a></p>
                                        <p>&copy; %d ATMate. Todos os direitos reservados.</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                        subject, // Para o <title>
                        clientName,
                        taxDescription,
                        taxReference,
                        deadlineStr,
                        // atmateClientAreaUrl, // Descomentar se usar o botão
                        "http://atmate.sytes.net/help",
                        "atmate.support@gmail.com",
                        "atmate.support@gmail.com",
                        "Não existe",
                        Year.now().getValue() // Ano atual para o copyright
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + contactType.getDescription());
        };
    }

}
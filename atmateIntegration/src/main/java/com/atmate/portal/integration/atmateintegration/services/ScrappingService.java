package com.atmate.portal.integration.atmateintegration.services;

import com.atmate.portal.integration.atmateintegration.beans.threads.GetATDataThread;
import com.atmate.portal.integration.atmateintegration.database.entitites.AtCredential;
import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.services.AtCredentialService;
import com.atmate.portal.integration.atmateintegration.database.services.ClientService;
import com.atmate.portal.integration.atmateintegration.utils.ProfileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;

@Service
public class ScrappingService {

    private static final Logger logger = LoggerFactory.getLogger(ScrappingService.class);

    @Autowired
    ClientService clientService;
    @Autowired
    AtCredentialService atCredentialService;
    @Autowired
    CryptoService cryptoService;
    @Autowired
    private ProfileUtil profileUtil;
    @Autowired
    private ApplicationContext applicationContext;


    // Cron: segundo minuto hora dia-do-mês mês dia-da-semana
    // "0 0 6 * * ?" significa:
    // - 0 segundos
    // - 0 minutos
    // - 6 horas (06:00)
    // - * (todos os dias do mês)
    // - * (todos os meses)
    // - ? (qualquer dia da semana - usado porque o dia do mês já é 'todos')
    // O fuso horário "Europe/Lisbon" garante que a tarefa corre às 06:00 hora de Portugal Continental.
    
    @Scheduled(cron = "0 0 6 * * ?", zone = "Europe/Lisbon")
    public void executeScrape() throws Exception {
        List<Client> clients = clientService.getAllClients();
        if (clients != null && !clients.isEmpty()) {
            logger.info("Número de clientes a processar: " + clients.size());
            if (!profileUtil.isDev()) {
                for (Client client : clients) {
                    logger.info("A iniciar tratamento do cliente " + client.getName() + " - NIF: " + client.getNif());
                    AtCredential atCredential = atCredentialService.getCredentialsByClientId(client);
                    if (atCredential != null) {
                        // Obter uma nova instância de GetATDataThread do ApplicationContext
                        GetATDataThread getATDataThread = applicationContext.getBean(GetATDataThread.class);
                        // Configurar a instância
                        getATDataThread.setClient(client);
                        getATDataThread.setGetTypeFromAT(true);
                        String decryptedPassword = cryptoService.decrypt(atCredential.getPassword());
                        getATDataThread.setPassword(decryptedPassword);
                        // Iniciar a thread
                        Thread thread = new Thread(getATDataThread);
                        thread.start();
                    }
                }
                logger.info("Scrapping finalizado com sucesso.");
            } else {
                logger.info("Perfil ativo é DEV, não será feito scrapping.");
            }
        }
    }

    public void syncClient(Client client, boolean getTypeFromAT) throws Exception {
        if (!profileUtil.isDev()) {
            logger.info("A iniciar sync do cliente NIF: " + client.getNif());
            AtCredential atCredential = atCredentialService.getCredentialsByClientId(client);
            if (atCredential != null) {
                // Obter uma nova instância de GetATDataThread do ApplicationContext
                GetATDataThread getATDataThread = applicationContext.getBean(GetATDataThread.class);
                // Configurar a instância
                getATDataThread.setClient(client);
                getATDataThread.setGetTypeFromAT(getTypeFromAT);
                String decryptedPassword = cryptoService.decrypt(atCredential.getPassword());
                getATDataThread.setPassword(decryptedPassword);
                // Iniciar a thread
                Thread thread = new Thread(getATDataThread);
                thread.start();
            }
            logger.info("Scrapping finalizado com sucesso.");
        } else {
            logger.info("Perfil ativo é DEV, não será feito scrapping.");
        }
    }
}
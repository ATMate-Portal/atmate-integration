package com.atmate.portal.integration.atmateintegration.services;

import com.atmate.portal.integration.atmateintegration.beans.threads.GetATDataThread;
import com.atmate.portal.integration.atmateintegration.database.entitites.AtCredential;
import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.services.AtCredentialService;
import com.atmate.portal.integration.atmateintegration.database.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScrappingService {

    @Autowired
    ClientService clientService;
    @Autowired
    AtCredentialService atCredentialService;
    @Autowired
    CryptoService cryptoService;
    @Autowired
    GetATDataThread getATDataThread;

    // Executa a cada 1 hora (3600000 ms)
    // Executa a cada 1 min (60000 ms)
    // Executa a cada 30 seg (30000 ms)
    // Executa a cada 1 seg (1000 ms)
    @Scheduled(fixedRate = 10000)
    public void executeScrape() throws Exception {

        List<Client> clients = clientService.getAllClients();

        for(Client client : clients){
            AtCredential atCredential = atCredentialService.getCredentialsByClientId(client);
            if( atCredential != null) {
                // Define os dados no objeto já gerenciado pelo Spring
                getATDataThread.setClient(client);

                String decryptedPassword = cryptoService.decrypt(atCredential.getPassword());

                getATDataThread.setPassword(decryptedPassword);

                // Correr a thread
                Thread thread = new Thread(getATDataThread);
                thread.start();
            }
        }
    }
}

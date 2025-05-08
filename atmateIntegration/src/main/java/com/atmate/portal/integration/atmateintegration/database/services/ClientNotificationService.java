package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.entitites.ClientNotification;
import com.atmate.portal.integration.atmateintegration.database.entitites.TaxType;
import com.atmate.portal.integration.atmateintegration.database.repos.ClientNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ClientNotificationService {

    private final ClientNotificationRepository clientNotificationRepository;

    @Autowired
    public ClientNotificationService(ClientNotificationRepository clientNotificationRepository) {
        this.clientNotificationRepository = clientNotificationRepository;
    }

    // Criar uma nova notificação do cliente
    public ClientNotification createClientNotification(ClientNotification clientNotification) {
        return clientNotificationRepository.save(clientNotification);
    }

    public List<ClientNotification> findPendingNotificationDueToday(){

        return clientNotificationRepository.findAllByStatus("PENDENTE");
    }
    // Ler todas as notificações do cliente
    public List<ClientNotification> getAllClientNotifications() {
        return clientNotificationRepository.findAll();
    }

    // Ler uma notificação do cliente por ID
    public Optional<ClientNotification> getClientNotificationById(Integer id) {
        return clientNotificationRepository.findById(id);
    }

    public boolean existsForClientTaxAndDate(Client client, TaxType taxType, LocalDate today){
        return clientNotificationRepository.existsClientNotificationByClientAndTaxTypeAndAndCreateDate(client, taxType, today);
    }

    // Atualizar uma notificação do cliente
    public ClientNotification updateClientNotification(Integer id, ClientNotification clientNotificationDetails) {
        ClientNotification clientNotification = clientNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação do cliente não encontrada com ID: " + id));

        clientNotification = clientNotificationDetails;

        return clientNotificationRepository.save(clientNotification);
    }

    // Deletar uma notificação do cliente
    public void deleteClientNotification(Integer id) {
        if (!clientNotificationRepository.existsById(id)) {
            throw new RuntimeException("Notificação do cliente não encontrada com ID: " + id);
        }
        clientNotificationRepository.deleteById(id);
    }
}

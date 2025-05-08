package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.ClientNotificationConfig;
import com.atmate.portal.integration.atmateintegration.database.repos.ClientNotificationConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientNotificationConfigService {

    private final ClientNotificationConfigRepository clientNotificationConfigRepository;

    @Autowired
    public ClientNotificationConfigService(ClientNotificationConfigRepository clientNotificationConfigRepository) {
        this.clientNotificationConfigRepository = clientNotificationConfigRepository;
    }

    // Criar uma nova configuração de notificação do cliente
    public ClientNotificationConfig createClientNotificationConfig(ClientNotificationConfig clientNotificationConfig) {
        return clientNotificationConfigRepository.save(clientNotificationConfig);
    }

    // Ler todas as configurações de notificação do cliente
    public List<ClientNotificationConfig> getAllClientNotificationConfigs() {
        return clientNotificationConfigRepository.findAll();
    }

    // Ler todas as configurações de notificação do cliente
    public List<ClientNotificationConfig> getAllActiveClientNotificationConfigs(boolean isActive) {
        return clientNotificationConfigRepository.getClientNotificationConfigByIsActiveEquals(isActive);
    }

    // Ler uma configuração de notificação do cliente por ID
    public Optional<ClientNotificationConfig> getClientNotificationConfigById(Integer id) {
        return clientNotificationConfigRepository.findById(id);
    }

    // Atualizar uma configuração de notificação do cliente
    public ClientNotificationConfig updateClientNotificationConfig(Integer id, ClientNotificationConfig clientNotificationConfigDetails) {
        ClientNotificationConfig clientNotificationConfig = clientNotificationConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração de notificação do cliente não encontrada com ID: " + id));

        clientNotificationConfig = clientNotificationConfigDetails;

        return clientNotificationConfigRepository.save(clientNotificationConfig);
    }

    // Deletar uma configuração de notificação do cliente
    public void deleteClientNotificationConfig(Integer id) {
        if (!clientNotificationConfigRepository.existsById(id)) {
            throw new RuntimeException("Configuração de notificação do cliente não encontrada com ID: " + id);
        }
        clientNotificationConfigRepository.deleteById(id);
    }
}

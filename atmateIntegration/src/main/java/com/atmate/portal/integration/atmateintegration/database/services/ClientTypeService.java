package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.ClientType;
import com.atmate.portal.integration.atmateintegration.database.repos.ClientTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class ClientTypeService {

    private final ClientTypeRepository clientTypeRepository;

    @Autowired
    public ClientTypeService(ClientTypeRepository clientTypeRepository) {
        this.clientTypeRepository = clientTypeRepository;
    }

    // Criar um novo tipo de cliente
    public ClientType createClientType(ClientType clientType) {
        return clientTypeRepository.save(clientType);
    }

    // Ler todos os tipos de clientes
    public List<ClientType> getAllClientTypes() {
        return clientTypeRepository.findAll();
    }

    // Ler um tipo de cliente por ID
    public Optional<ClientType> getClientTypeById(Integer id) {
        return clientTypeRepository.findById(id);
    }

    // Atualizar um tipo de cliente
    public ClientType updateClientType(Integer id, ClientType clientTypeDetails) {
        ClientType clientType = clientTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de cliente não encontrado com ID: " + id));

        // Atualizar os campos do tipo de cliente
        clientType.setDescription(clientTypeDetails.getDescription());
        // Adicione outros campos conforme necessário

        return clientTypeRepository.save(clientType);
    }

    // Deletar um tipo de cliente
    public void deleteClientType(Integer id) {
        if (!clientTypeRepository.existsById(id)) {
            throw new RuntimeException("Tipo de cliente não encontrado com ID: " + id);
        }
        clientTypeRepository.deleteById(id);
    }
}


package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.OperationHistory;
import com.atmate.portal.integration.atmateintegration.database.repos.OperationHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OperationHistoryService {

    private final OperationHistoryRepository operationHistoryRepository;

    @Autowired
    public OperationHistoryService(OperationHistoryRepository operationHistoryRepository) {
        this.operationHistoryRepository = operationHistoryRepository;
    }

    // Criar um novo registro de histórico de operações
    public OperationHistory createOperationHistory(OperationHistory operationHistory) {
        return operationHistoryRepository.save(operationHistory);
    }

    // Ler todos os registros de histórico de operações
    public List<OperationHistory> getAllOperationHistories() {
        return operationHistoryRepository.findAll();
    }

    // Ler um registro de histórico de operações por ID
    public Optional<OperationHistory> getOperationHistoryById(Integer id) {
        return operationHistoryRepository.findById(id);
    }

    // Atualizar um registro de histórico de operações
    public OperationHistory updateOperationHistory(Integer id, OperationHistory operationHistoryDetails) {
        OperationHistory operationHistory = operationHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Histórico de operação não encontrado com ID: " + id));

        // Atualizar os campos do histórico de operação
        operationHistory.setUser(operationHistoryDetails.getUser());
        operationHistory.setUserAction(operationHistoryDetails.getUserAction());
        // Adicione outros campos conforme necessário

        return operationHistoryRepository.save(operationHistory);
    }

    // Deletar um registro de histórico de operações
    public void deleteOperationHistory(Integer id) {
        if (!operationHistoryRepository.existsById(id)) {
            throw new RuntimeException("Histórico de operação não encontrado com ID: " + id);
        }
        operationHistoryRepository.deleteById(id);
    }
}

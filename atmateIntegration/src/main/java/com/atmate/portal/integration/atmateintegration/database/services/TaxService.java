package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.entitites.Tax;
import com.atmate.portal.integration.atmateintegration.database.entitites.TaxType;
import com.atmate.portal.integration.atmateintegration.database.repos.TaxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaxService {

    private final TaxRepository taxRepository;

    @Autowired
    public TaxService(TaxRepository taxRepository) {
        this.taxRepository = taxRepository;
    }

    // Criar um novo imposto
    public Tax createTax(Tax tax) {
        return taxRepository.save(tax);
    }

    // Ler todos os impostos
    public List<Tax> getAllTaxes() {
        return taxRepository.findAll();
    }

    // Ler um imposto por ID
    public Optional<Tax> getTaxById(Integer id) {
        return taxRepository.findById(id);
    }

    public Tax getTaxByClientAndType(Client client, TaxType taxType, String identificador) throws JsonProcessingException {

        List<Tax> taxClientList = taxRepository.findTaxByClientAndTaxType(client, taxType);

            switch (taxType.getId()) {
                case 1:

                    for (Tax clientTax : taxClientList) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(clientTax.getTaxData());
                        String matricula = rootNode.get("Matrícula").asText();

                        if (matricula.equals(identificador)) {
                            return clientTax;
                        }
                    }
                    break;
                case 5:

                    for (Tax clientTax : taxClientList) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(clientTax.getTaxData());
                        String notaCobranca = rootNode.get("Nº Nota Cob.").asText();

                        if (notaCobranca.equals(identificador)) {
                            return clientTax;
                        }
                    }
                    break;
            }


        return null;
    }

    public List<Tax> getTaxesByClientAndType(Client client, TaxType taxType) throws JsonProcessingException {
        return taxRepository.findTaxByClientAndTaxType(client, taxType);
    }

    // Atualizar um imposto
    public Tax updateTax(Integer id, Tax taxDetails) {
        Tax tax = taxRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imposto não encontrado com ID: " + id));

        tax.setTaxType(taxDetails.getTaxType());
        tax.setTaxData(taxDetails.getTaxData());
        tax.setUpdatedAt(LocalDateTime.now());


        return taxRepository.save(tax);
    }

    // Deletar um imposto
    public void deleteTax(Integer id) {
        if (!taxRepository.existsById(id)) {
            throw new RuntimeException("Imposto não encontrado com ID: " + id);
        }
        taxRepository.deleteById(id);
    }
}

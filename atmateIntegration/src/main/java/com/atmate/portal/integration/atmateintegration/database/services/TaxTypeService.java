package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.TaxType;
import com.atmate.portal.integration.atmateintegration.database.repos.TaxTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaxTypeService {

    private final TaxTypeRepository taxTypeRepository;

    @Autowired
    public TaxTypeService(TaxTypeRepository taxTypeRepository) {
        this.taxTypeRepository = taxTypeRepository;
    }

    // Criar um novo tipo de imposto
    public TaxType createTaxType(TaxType taxType) {
        return taxTypeRepository.save(taxType);
    }

    // Ler todos os tipos de imposto
    public List<TaxType> getAllTaxTypes() {
        return taxTypeRepository.findAll();
    }

    // Ler um tipo de imposto por ID
    public Optional<TaxType> getTaxTypeById(Integer id) {
        return taxTypeRepository.findById(id);
    }

    // Atualizar um tipo de imposto
    public TaxType updateTaxType(Integer id, TaxType taxTypeDetails) {
        TaxType taxType = taxTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de imposto não encontrado com ID: " + id));

        // Atualizar os campos do tipo de imposto
        taxType.setDescription(taxTypeDetails.getDescription());
        // Adicione outros campos conforme necessário

        return taxTypeRepository.save(taxType);
    }

    // Deletar um tipo de imposto
    public void deleteTaxType(Integer id) {
        if (!taxTypeRepository.existsById(id)) {
            throw new RuntimeException("Tipo de imposto não encontrado com ID: " + id);
        }
        taxTypeRepository.deleteById(id);
    }
}

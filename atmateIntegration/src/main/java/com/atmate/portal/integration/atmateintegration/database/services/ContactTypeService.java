package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.ContactType;
import com.atmate.portal.integration.atmateintegration.database.repos.ContactTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactTypeService {

    private final ContactTypeRepository contactTypeRepository;

    @Autowired
    public ContactTypeService(ContactTypeRepository contactTypeRepository) {
        this.contactTypeRepository = contactTypeRepository;
    }

    // Criar um novo tipo de contato
    public ContactType createContactType(ContactType contactType) {
        return contactTypeRepository.save(contactType);
    }

    // Ler todos os tipos de contato
    public List<ContactType> getAllContactTypes() {
        return contactTypeRepository.findAll();
    }

    // Ler um tipo de contato por ID
    public Optional<ContactType> getContactTypeById(Integer id) {
        return contactTypeRepository.findById(id);
    }

    // Atualizar um tipo de contato
    public ContactType updateContactType(Integer id, ContactType contactTypeDetails) {
        ContactType contactType = contactTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de contato não encontrado com ID: " + id));

        // Atualizar os campos do tipo de contato
        contactType.setDescription(contactTypeDetails.getDescription());
        // Adicione outros campos conforme necessário

        return contactTypeRepository.save(contactType);
    }

    // Deletar um tipo de contato
    public void deleteContactType(Integer id) {
        if (!contactTypeRepository.existsById(id)) {
            throw new RuntimeException("Tipo de contato não encontrado com ID: " + id);
        }
        contactTypeRepository.deleteById(id);
    }
}

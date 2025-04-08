package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.Contact;
import com.atmate.portal.integration.atmateintegration.database.repos.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactService {

    private final ContactRepository contactRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    // Criar um novo contato
    public Contact createContact(Contact contact) {
        return contactRepository.save(contact);
    }

    // Ler todos os contatos
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    // Ler um contato por ID
    public Optional<Contact> getContactById(Integer id) {
        return contactRepository.findById(id);
    }

    // Atualizar um contato
    public Contact updateContact(Integer id, Contact contactDetails) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contato não encontrado com ID: " + id));

        contact = contactDetails;

        return contactRepository.save(contact);
    }

    // Deletar um contato
    public void deleteContact(Integer id) {
        if (!contactRepository.existsById(id)) {
            throw new RuntimeException("Contato não encontrado com ID: " + id);
        }
        contactRepository.deleteById(id);
    }

    public boolean existsContactForClient(Contact contact) {
        return contactRepository.existsByContactAndContactTypeAndClient(
                contact.getContact(),
                contact.getContactType(),
                contact.getClient()
        );
    }
}

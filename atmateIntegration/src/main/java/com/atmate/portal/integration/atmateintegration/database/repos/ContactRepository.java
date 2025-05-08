package com.atmate.portal.integration.atmateintegration.database.repos;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.entitites.Contact;
import com.atmate.portal.integration.atmateintegration.database.entitites.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {
    boolean existsByContactAndContactTypeAndClient(String contact, ContactType contactType, Client client);

    Contact findByClientAndContactType(Client client, ContactType contactType);
}


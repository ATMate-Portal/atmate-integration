package com.atmate.portal.integration.atmateintegration.database.repos;

import com.atmate.portal.integration.atmateintegration.database.entitites.AtCredential;
import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtCredentialRepository extends JpaRepository<AtCredential, Integer> {

    AtCredential findByClient(Client client);

}

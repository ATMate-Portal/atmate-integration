package com.atmate.portal.integration.atmateintegration.database.repos;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.entitites.ClientNotification;
import com.atmate.portal.integration.atmateintegration.database.entitites.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientNotificationRepository extends JpaRepository<ClientNotification, Integer> {
    // Você pode adicionar métodos personalizados aqui, se necessário

    boolean existsClientNotificationByClientAndTaxTypeAndAndCreateDate(Client client, TaxType taxType, LocalDate today);

    List<ClientNotification> findAllByStatus(String status);
}


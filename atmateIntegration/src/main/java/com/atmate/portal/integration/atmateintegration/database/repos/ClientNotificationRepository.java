package com.atmate.portal.integration.atmateintegration.database.repos;

import com.atmate.portal.integration.atmateintegration.database.entitites.ClientNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientNotificationRepository extends JpaRepository<ClientNotification, Integer> {
    // Você pode adicionar métodos personalizados aqui, se necessário
}


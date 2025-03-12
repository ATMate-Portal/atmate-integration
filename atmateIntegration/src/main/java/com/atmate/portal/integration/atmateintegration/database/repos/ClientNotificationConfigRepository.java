package com.atmate.portal.integration.atmateintegration.database.repos;

import com.atmate.portal.integration.atmateintegration.database.entitites.ClientNotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientNotificationConfigRepository extends JpaRepository<ClientNotificationConfig, Integer> {
    // Você pode adicionar métodos personalizados aqui, se necessário
}
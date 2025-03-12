package com.atmate.portal.integration.atmateintegration.database.repos;

import com.atmate.portal.integration.atmateintegration.database.entitites.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    // Você pode adicionar métodos personalizados aqui, se necessário
}

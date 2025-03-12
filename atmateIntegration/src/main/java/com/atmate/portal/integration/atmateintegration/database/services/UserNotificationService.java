package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.UserNotification;
import com.atmate.portal.integration.atmateintegration.database.repos.UserNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;

    @Autowired
    public UserNotificationService(UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    // Criar uma nova notificação de usuário
    public UserNotification createUserNotification(UserNotification userNotification) {
        return userNotificationRepository.save(userNotification);
    }

    // Ler todas as notificações de usuários
    public List<UserNotification> getAllUserNotifications() {
        return userNotificationRepository.findAll();
    }

    // Ler uma notificação de usuário por ID
    public Optional<UserNotification> getUserNotificationById(Integer id) {
        return userNotificationRepository.findById(id);
    }

    // Atualizar uma notificação de usuário
    public UserNotification updateUserNotification(Integer id, UserNotification userNotificationDetails) {
        UserNotification userNotification = userNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação de usuário não encontrada com ID: " + id));

        // Atualizar os campos da notificação de usuário
        userNotification.setTitle(userNotificationDetails.getTitle());
        userNotification.setMessage(userNotificationDetails.getMessage());
        userNotification.setSendDate(userNotificationDetails.getSendDate());
        userNotification.setIsRead(userNotificationDetails.getIsRead());
        userNotification.setReadDate(userNotificationDetails.getReadDate());
        userNotification.setIsUrgent(userNotificationDetails.getIsUrgent());

        return userNotificationRepository.save(userNotification);
    }

    // Deletar uma notificação de usuário
    public void deleteUserNotification(Integer id) {
        if (!userNotificationRepository.existsById(id)) {
            throw new RuntimeException("Notificação de usuário não encontrada com ID: " + id);
        }
        userNotificationRepository.deleteById(id);
    }
}

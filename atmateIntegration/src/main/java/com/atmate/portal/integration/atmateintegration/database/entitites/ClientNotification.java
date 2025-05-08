package com.atmate.portal.integration.atmateintegration.database.entitites;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ClientNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "notification_type", nullable = false)
    private ContactType notificationType;

    @ManyToOne
    @JoinColumn(name = "tax", nullable = false)
    private TaxType taxType;

    @ManyToOne
    @JoinColumn(name = "client_notification_config", nullable = false)
    private ClientNotificationConfig clientNotificationConfig;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "O status é obrigatório")
    @Size(max = 100, message = "O status deve ter no máximo 100 caracteres")
    private String status;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "O título é obrigatório")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
    private String title;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "A mensagem é obrigatória")
    @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres")
    private String message;

    @Column(name = "created_date")
    private LocalDate createDate;

    @Column(name = "send_date")
    private LocalDateTime sendDate;
}


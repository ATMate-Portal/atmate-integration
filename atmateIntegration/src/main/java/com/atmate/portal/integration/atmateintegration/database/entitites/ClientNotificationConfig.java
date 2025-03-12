package com.atmate.portal.integration.atmateintegration.database.entitites;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_notifications_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ClientNotificationConfig {

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
    @JoinColumn(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(length = 50)
    @Size(max = 50, message = "A frequência deve ter no máximo 50 caracteres")
    private String frequency;

    @Column(name = "start_period")
    private Byte startPeriod;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


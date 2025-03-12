package com.atmate.portal.integration.atmateintegration.database.entitites;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_action", nullable = false, length = 100)
    @NotBlank(message = "A ação do usuário é obrigatória")
    @Size(max = 100, message = "A ação do usuário deve ter no máximo 100 caracteres")
    private String userAction;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}


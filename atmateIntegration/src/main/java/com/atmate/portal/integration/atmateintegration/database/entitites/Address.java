package com.atmate.portal.integration.atmateintegration.database.entitites;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "A rua é obrigatória")
    @Size(max = 255, message = "A rua deve ter no máximo 255 caracteres")
    private String street;

    @Column(length = 10)
    @Size(max = 10, message = "O número da porta deve ter no máximo 10 caracteres")
    private String doorNumber;

    @Column(length = 20)
    @Size(max = 20, message = "O código postal deve ter no máximo 20 caracteres")
    private String zipCode;

    @Column(length = 100)
    @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
    private String city;

    @Column(length = 100)
    @Size(max = 100, message = "O concelho deve ter no máximo 100 caracteres")
    private String county;

    @Column(length = 100)
    @Size(max = 100, message = "O distrito deve ter no máximo 100 caracteres")
    private String district;

    @Column(length = 100)
    @Size(max = 100, message = "A freguesia deve ter no máximo 100 caracteres")
    private String parish;

    @Column(length = 50)
    @Size(max = 50, message = "O país deve ter no máximo 50 caracteres")
    private String country;

    @ManyToOne
    @JoinColumn(name = "address_type", nullable = false)
    private AddressType addressType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


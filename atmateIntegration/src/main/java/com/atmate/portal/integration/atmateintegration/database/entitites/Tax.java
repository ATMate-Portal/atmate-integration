package com.atmate.portal.integration.atmateintegration.database.entitites;

import com.atmate.portal.integration.atmateintegration.utils.enums.ErrorEnum;
import com.atmate.portal.integration.atmateintegration.utils.exceptions.ATMateException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "taxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Tax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "tax_type", nullable = false)
    private TaxType taxType;

    @Column(name = "tax_data", columnDefinition = "json")
    private String taxData; // Se necessário, pode ser um objeto que representa o JSON

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "payment_deadline", insertable = false, updatable = false)
    private LocalDate paymentDeadline;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getIdentifier(JsonNode jsonNode) {
        return switch (taxType.getId()) {
            case 1 -> jsonNode.path("Matrícula").asText(); //IUC
            case 5 -> jsonNode.path("Nº Nota Cob.").asText(); //IMI
            default -> throw new ATMateException(ErrorEnum.INVALID_TAX_TYPE);
        };
    }
}


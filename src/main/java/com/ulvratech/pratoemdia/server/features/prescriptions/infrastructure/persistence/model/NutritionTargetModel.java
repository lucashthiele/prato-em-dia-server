package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescription_nutrition_target")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionTargetModel {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_version_id", nullable = false)
    private PrescriptionVersionModel version;
    @Column(name = "nutrient_code", nullable = false, length = 64)
    private String nutrientCode;
    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 16)
    private String unit;
}

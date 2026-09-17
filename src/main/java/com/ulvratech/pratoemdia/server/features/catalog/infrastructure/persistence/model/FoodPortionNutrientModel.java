package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "food_portion_nutrient", uniqueConstraints = @UniqueConstraint(name = "uq_food_portion_nutrient_portion_code", columnNames = {
        "food_portion_id", "nutrient_code" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPortionNutrientModel {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_portion_id", nullable = false)
    private FoodPortionModel portion;

    @Column(name = "nutrient_code", nullable = false, length = 64)
    private String nutrientCode;

    @Column(precision = 38, scale = 18)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_status", nullable = false, length = 32)
    private NutrientValueStatus valueStatus;

    @Column(name = "raw_value", length = 64)
    private String rawValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

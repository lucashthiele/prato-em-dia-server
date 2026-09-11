package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "food_nutrient",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_food_nutrient_food_code",
        columnNames = {"food_id", "nutrient_code"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodNutrientModel {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private FoodModel food;

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

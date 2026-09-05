package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "food_portion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPortionModel {
    @Id
    private UUID id;

    @Column(name = "food_id", nullable = false)
    private UUID foodId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 32)
    private String unit;

    @Column(name = "grams_equivalent", precision = 12, scale = 3)
    private BigDecimal gramsEquivalent;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

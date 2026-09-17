package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

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

    @Column(length = 32)
    private String unit;

    @Column(name = "grams_equivalent", precision = 12, scale = 3)
    private BigDecimal gramsEquivalent;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "raw_label", length = 100)
    private String rawLabel;

    @OneToMany(mappedBy = "portion", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<FoodPortionNutrientModel> nutrients = new ArrayList<>();

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

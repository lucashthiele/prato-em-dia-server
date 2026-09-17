package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "allowed_substitution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowedSubstitutionModel {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescribed_meal_item_id", nullable = false)
    private PrescribedMealItemModel item;
    @Column(name = "substitute_food_id", nullable = false)
    private UUID foodId;
    @Column(name = "substitute_food_portion_id")
    private UUID foodPortionId;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Column(nullable = false, length = 32)
    private String unit;
    @Column(columnDefinition = "text")
    private String notes;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

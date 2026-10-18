package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meal_execution_nutrient_snapshot")
@IdClass(MealExecutionNutrientSnapshotModel.Key.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealExecutionNutrientSnapshotModel {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private MealExecutionItemModel item;

    @Id
    @Column(name = "nutrient_code", nullable = false, length = 64)
    private String nutrientCode;

    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String unit;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID item;
        private String nutrientCode;
    }
}

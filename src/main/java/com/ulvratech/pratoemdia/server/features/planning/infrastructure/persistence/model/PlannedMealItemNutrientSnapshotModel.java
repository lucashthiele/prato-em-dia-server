package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** Maintained by the database trigger on planned_meal_item; read-only here. */
@Entity
@Immutable
@Table(name = "planned_meal_item_nutrient_snapshot")
@IdClass(PlannedMealItemNutrientSnapshotModel.Key.class)
@Getter
@NoArgsConstructor
public class PlannedMealItemNutrientSnapshotModel {
    @Id
    @Column(name = "planned_item_id")
    private UUID plannedItemId;

    @Id
    @Column(name = "nutrient_code", length = 64)
    private String nutrientCode;

    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String unit;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID plannedItemId;
        private String nutrientCode;
    }
}

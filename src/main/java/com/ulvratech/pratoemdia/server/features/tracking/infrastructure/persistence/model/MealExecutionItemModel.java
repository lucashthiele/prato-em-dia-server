package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "meal_execution_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealExecutionItemModel {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id")
    private MealExecutionModel execution;

    @Column(name = "item_order", nullable = false)
    private int order;

    @Column(name = "planned_item_id")
    private UUID plannedItemId;

    @Column(name = "allowed_substitution_id")
    private UUID allowedSubstitutionId;

    @Column(name = "food_id", nullable = false)
    private UUID foodId;

    @Column(name = "food_portion_id", nullable = false)
    private UUID foodPortionId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal multiplier;

    @Column(nullable = false, length = 32)
    private String unit;

    @Column(precision = 16, scale = 4)
    private BigDecimal grams;

    @Column(precision = 16, scale = 4)
    private BigDecimal kcal;

    @OneToMany(mappedBy = "item", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 200)
    @Builder.Default
    private List<MealExecutionNutrientSnapshotModel> nutrients = new ArrayList<>();
}

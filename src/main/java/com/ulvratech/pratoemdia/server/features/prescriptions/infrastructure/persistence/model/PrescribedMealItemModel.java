package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescribed_meal_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescribedMealItemModel {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescribed_meal_id", nullable = false)
    private PrescribedMealModel meal;
    @Column(name = "food_id", nullable = false)
    private UUID foodId;
    @Column(name = "food_portion_id")
    private UUID foodPortionId;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Column(nullable = false, length = 32)
    private String unit;
    @Column(name = "item_order", nullable = false)
    private int order;
    @Column(columnDefinition = "text")
    private String notes;
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<AllowedSubstitutionModel> substitutions = new ArrayList<>();
}

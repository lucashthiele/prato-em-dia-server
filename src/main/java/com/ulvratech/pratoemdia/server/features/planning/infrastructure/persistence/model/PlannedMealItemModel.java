package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "planned_meal_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedMealItemModel {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "planned_meal_id")
  private PlannedMealModel meal;

  @Column(name = "prescribed_meal_item_id")
  private UUID prescribedMealItemId;

  @Column(name = "allowed_substitution_id")
  private UUID allowedSubstitutionId;

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
}

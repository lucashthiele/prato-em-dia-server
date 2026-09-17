package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "planned_meal")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedMealModel {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "weekly_plan_id")
  private WeeklyPlanModel plan;

  @Column(name = "prescribed_meal_id")
  private UUID prescribedMealId;

  @Column(name = "meal_date", nullable = false)
  private LocalDate date;

  @Column(name = "meal_time")
  private LocalTime time;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "meal_type", nullable = false)
  private MealType mealType;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "meal_order", nullable = false)
  private int order;

  @OneToMany(mappedBy = "meal", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 200)
  @Builder.Default
  private List<PlannedMealItemModel> items = new ArrayList<>();
}

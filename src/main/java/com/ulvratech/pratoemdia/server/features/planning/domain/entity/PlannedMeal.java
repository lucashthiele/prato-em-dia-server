package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlannedMeal(
    UUID id,
    UUID prescribedMealId,
    LocalDate date,
    LocalTime time,
    MealType mealType,
    String name,
    int order,
    List<PlannedMealItem> items) {
  public PlannedMeal {
    Objects.requireNonNull(id, "Meal id is required");
    Objects.requireNonNull(date, "Meal date is required");
    Objects.requireNonNull(mealType, "Meal type is required");
    if (name == null || name.isBlank() || name.length() > 120)
      throw new IllegalArgumentException("Meal name is required");
    if (order < 0) throw new IllegalArgumentException("Meal order cannot be negative");
    name = name.trim();
    items = List.copyOf(items == null ? List.of() : items);
    if (items.stream().map(PlannedMealItem::id).distinct().count() != items.size()
        || items.stream().map(PlannedMealItem::order).distinct().count() != items.size())
      throw new IllegalArgumentException("Duplicate planned item");
  }
}

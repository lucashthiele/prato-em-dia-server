package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PrescribedMealItem(
    UUID id,
    UUID foodId,
    UUID foodPortionId,
    BigDecimal quantity,
    String unit,
    int order,
    String notes,
    List<AllowedSubstitution> substitutions) {
  public PrescribedMealItem {
    if (id == null || foodId == null)
      throw new IllegalArgumentException("Meal item identifiers are required");
    if (quantity == null || quantity.signum() <= 0)
      throw new IllegalArgumentException("Meal item quantity must be positive");
    unit = AllowedSubstitution.normalized(unit);
    if (unit == null || unit.length() > 32)
      throw new IllegalArgumentException("Meal item unit is required");
    if (order < 0) throw new IllegalArgumentException("Meal item order cannot be negative");
    notes = AllowedSubstitution.normalized(notes);
    substitutions = List.copyOf(substitutions == null ? List.of() : substitutions);
    long distinct =
        substitutions.stream()
            .map(substitution -> substitution.foodId() + ":" + substitution.foodPortionId())
            .distinct()
            .count();
    if (distinct != substitutions.size())
      throw new IllegalArgumentException("Duplicate substitution");
  }

  public static PrescribedMealItem create(
      UUID foodId,
      UUID portionId,
      BigDecimal quantity,
      String unit,
      int order,
      String notes,
      List<AllowedSubstitution> substitutions) {
    return new PrescribedMealItem(
        UUID.randomUUID(), foodId, portionId, quantity, unit, order, notes, substitutions);
  }
}

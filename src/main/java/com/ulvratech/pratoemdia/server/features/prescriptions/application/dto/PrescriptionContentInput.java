package com.ulvratech.pratoemdia.server.features.prescriptions.application.dto;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PrescriptionContentInput(String title, String notes, List<MealInput> meals) {
  public PrescriptionContentInput {
    meals = List.copyOf(meals == null ? List.of() : meals);
  }

  public record MealInput(
      String name,
      MealType mealType,
      int order,
      LocalTime suggestedTime,
      String notes,
      List<ItemInput> items) {
    public MealInput {
      items = List.copyOf(items == null ? List.of() : items);
    }

    public MealInput(
        String name, int order, LocalTime suggestedTime, String notes, List<ItemInput> items) {
      this(name, MealType.SNACK, order, suggestedTime, notes, items);
    }
  }

  public record ItemInput(
      UUID foodId,
      UUID foodPortionId,
      BigDecimal quantity,
      String unit,
      int order,
      String notes,
      List<SubstitutionInput> substitutions) {
    public ItemInput {
      substitutions = List.copyOf(substitutions == null ? List.of() : substitutions);
    }
  }

  public record SubstitutionInput(
      UUID foodId, UUID foodPortionId, BigDecimal quantity, String unit, String notes) {}
}

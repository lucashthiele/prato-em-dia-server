package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;

import java.time.Instant;
import java.util.UUID;

public record FoodOutput(
    UUID id,
    String name,
    String category,
    String defaultUnit,
    String notes,
    String preparationMethod,
    boolean active,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt,
    long version
) {
    public static FoodOutput from(Food food) {
        return new FoodOutput(
            food.getId(), food.getName(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getCreatedAt(), food.getUpdatedAt(), food.getVersion()
        );
    }
}

package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;

import java.time.Instant;
import java.util.UUID;

public record FoodOutput(
    UUID id,
    String name,
    String brand,
    String category,
    String defaultUnit,
    String notes,
    String preparationMethod,
    boolean active,
    UUID createdByUserId,
    FoodOrigin origin,
    Instant createdAt,
    Instant updatedAt,
    long version,
    String source,
    String sourceExternalId,
    String sourceDescription,
    String sourceVersion,
    String normalizedName
) {
    public static FoodOutput from(Food food) {
        return new FoodOutput(
            food.getId(), food.getName(), food.getBrand(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getOrigin(), food.getCreatedAt(), food.getUpdatedAt(),
            food.getVersion(), food.getSource(), food.getSourceExternalId(),
            food.getSourceDescription(), food.getSourceVersion(), food.getNormalizedName()
        );
    }
}

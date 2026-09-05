package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FoodDetailsOutput(
    UUID id,
    String name,
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
    List<FoodPortionOutput> portions
) {
    public static FoodDetailsOutput from(Food food, List<FoodPortion> portions) {
        return new FoodDetailsOutput(
            food.getId(), food.getName(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getOrigin(), food.getCreatedAt(), food.getUpdatedAt(),
            food.getVersion(), portions.stream().map(FoodPortionOutput::from).toList()
        );
    }
}

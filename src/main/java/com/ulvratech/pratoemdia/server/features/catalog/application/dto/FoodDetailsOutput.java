package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;

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
    String source,
    String sourceExternalId,
    String sourceDescription,
    String sourceVersion,
    String normalizedName,
    Map<String, NutrientValue> nutrients,
    List<FoodPortionOutput> portions
) {
    public static FoodDetailsOutput from(Food food, List<FoodPortion> portions) {
        return new FoodDetailsOutput(
            food.getId(), food.getName(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getOrigin(), food.getCreatedAt(), food.getUpdatedAt(),
            food.getVersion(), food.getSource(), food.getSourceExternalId(),
            food.getSourceDescription(), food.getSourceVersion(), food.getNormalizedName(),
            food.getNutrients(), portions.stream().map(FoodPortionOutput::from).toList()
        );
    }
}

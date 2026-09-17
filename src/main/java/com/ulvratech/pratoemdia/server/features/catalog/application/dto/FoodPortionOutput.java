package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;

public record FoodPortionOutput(
        UUID id,
        UUID foodId,
        String label,
        BigDecimal quantity,
        String unit,
        BigDecimal gramsEquivalent,
        String type,
        String rawLabel,
        Map<String, NutrientValue> nutrients,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
    public static FoodPortionOutput from(FoodPortion portion) {
        return new FoodPortionOutput(
                portion.getId(), portion.getFoodId(), portion.getLabel(), portion.getQuantity(),
                portion.getUnit(), portion.getGramsEquivalent(), portion.getSourceType(),
                portion.getRawLabel(), portion.getNutrients(), portion.isActive(),
                portion.getCreatedAt(), portion.getUpdatedAt()
        );
    }
}

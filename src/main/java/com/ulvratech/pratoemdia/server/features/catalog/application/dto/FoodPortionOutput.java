package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FoodPortionOutput(
    UUID id,
    UUID foodId,
    String label,
    BigDecimal quantity,
    String unit,
    BigDecimal gramsEquivalent,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static FoodPortionOutput from(FoodPortion portion) {
        return new FoodPortionOutput(
            portion.getId(), portion.getFoodId(), portion.getLabel(), portion.getQuantity(),
            portion.getUnit(), portion.getGramsEquivalent(), portion.isActive(),
            portion.getCreatedAt(), portion.getUpdatedAt()
        );
    }
}

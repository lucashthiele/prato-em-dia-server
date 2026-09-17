package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PlannedMealItem(
        UUID id,
        UUID prescribedMealItemId,
        UUID allowedSubstitutionId,
        UUID foodId,
        UUID foodPortionId,
        BigDecimal quantity,
        String unit,
        int order) {
    public PlannedMealItem {
        Objects.requireNonNull(id, "Item id is required");
        Objects.requireNonNull(foodId, "Food is required");
        if (quantity == null || quantity.signum() <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        if (unit == null || unit.isBlank() || unit.length() > 32)
            throw new IllegalArgumentException("Unit is required");
        if (order < 0)
            throw new IllegalArgumentException("Item order cannot be negative");
        unit = unit.trim();
    }
}

package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;

import java.math.BigDecimal;

public record NutrientValue(
    BigDecimal amount,
    String unit,
    NutrientValueStatus status,
    String rawValue
) {
    public NutrientValue {
        if (unit == null || unit.isBlank()) throw new IllegalArgumentException("unit is required");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (status == NutrientValueStatus.VALUE && amount == null) {
            throw new IllegalArgumentException("amount is required for a numeric nutrient");
        }
        if (status != NutrientValueStatus.VALUE && amount != null) {
            throw new IllegalArgumentException("amount is only allowed for numeric nutrients");
        }
        if (amount != null) amount = amount.stripTrailingZeros();
        unit = unit.trim();
    }
}

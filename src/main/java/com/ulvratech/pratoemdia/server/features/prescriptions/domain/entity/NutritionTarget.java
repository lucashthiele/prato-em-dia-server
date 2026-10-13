package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import java.math.BigDecimal;

public record NutritionTarget(String nutrientCode, BigDecimal amount, String unit) {
    public NutritionTarget {
        if (nutrientCode == null || nutrientCode.isBlank() || nutrientCode.length() > 64
                || amount == null || amount.signum() < 0 || unit == null || unit.isBlank()
                || unit.length() > 16)
            throw new IllegalArgumentException("Invalid nutrition target");
        nutrientCode = nutrientCode.trim();
        unit = unit.trim();
    }
}

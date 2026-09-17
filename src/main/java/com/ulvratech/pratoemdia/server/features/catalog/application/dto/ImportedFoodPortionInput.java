package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;

import java.math.BigDecimal;
import java.util.Map;

public record ImportedFoodPortionInput(
        String type,
        String label,
        String rawLabel,
        BigDecimal quantity,
        String unit,
        Map<String, NutrientValue> nutrients) {
    public ImportedFoodPortionInput {
        nutrients = nutrients == null ? Map.of() : Map.copyOf(nutrients);
    }
}

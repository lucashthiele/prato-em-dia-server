package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;

import java.util.Map;

public record ImportedFoodInput(
    String externalId,
    String description,
    String category,
    Map<String, NutrientValue> nutrients
) {
    public ImportedFoodInput {
        nutrients = Map.copyOf(nutrients);
    }
}

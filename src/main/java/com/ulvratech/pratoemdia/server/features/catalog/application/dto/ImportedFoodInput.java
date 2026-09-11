package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import java.util.List;

public record ImportedFoodInput(
    String externalId,
    String description,
    String category,
    String brand,
    List<ImportedFoodPortionInput> portions
) {
    public ImportedFoodInput {
        portions = portions == null ? List.of() : List.copyOf(portions);
    }
}

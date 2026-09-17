package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import java.util.List;

public record SyncImportedFoodsInput(
        String source,
        String sourceVersion,
        List<ImportedFoodInput> foods,
        boolean dryRun) {
    public SyncImportedFoodsInput {
        foods = List.copyOf(foods);
    }
}

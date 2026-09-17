package com.ulvratech.pratoemdia.server.features.importer.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodPortionInput;

import java.util.List;

public record MongoFoodRecord(
        String externalId,
        String name,
        String brand,
        String category,
        List<ImportedFoodPortionInput> portions) {
    public MongoFoodRecord {
        portions = List.copyOf(portions);
    }
}

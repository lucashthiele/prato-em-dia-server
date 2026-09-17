package com.ulvratech.pratoemdia.server.features.importer.application.dto;

import java.util.List;

public record MongoCatalogData(
        int documentsRead,
        String checksum,
        List<MongoFoodRecord> records,
        List<CatalogImportIssue> issues) {
    public MongoCatalogData {
        records = List.copyOf(records);
        issues = List.copyOf(issues);
    }
}

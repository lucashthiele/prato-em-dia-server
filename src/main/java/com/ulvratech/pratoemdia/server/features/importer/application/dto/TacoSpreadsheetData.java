package com.ulvratech.pratoemdia.server.features.importer.application.dto;

import java.util.List;

public record TacoSpreadsheetData(
    int rowsRead,
    List<TacoFoodRecord> records,
    List<TacoImportIssue> issues
) {
    public TacoSpreadsheetData {
        records = List.copyOf(records);
        issues = List.copyOf(issues);
    }
}

package com.ulvratech.pratoemdia.server.features.importer.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportTacoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoImportReport;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportTacoCatalog;

public class ImportTacoCatalogUseCase implements ImportTacoCatalog {
    public static final String SOURCE = "TACO";
    public static final String SOURCE_VERSION = "TACO 4a edicao (2011)";

    private final SyncImportedFoods catalog;

    public ImportTacoCatalogUseCase(SyncImportedFoods catalog) {
        this.catalog = catalog;
    }

    @Override
    public TacoImportReport execute(ImportTacoCatalogInput input) {
        var spreadsheet = input.spreadsheet();
        boolean aborted = !input.dryRun() && !spreadsheet.issues().isEmpty();
        var foods = spreadsheet.records().stream()
            .map(record -> new ImportedFoodInput(
                record.externalId(), record.description(), record.category(), record.nutrients()
            ))
            .toList();
        var result = catalog.execute(new SyncImportedFoodsInput(
            SOURCE, SOURCE_VERSION, foods, input.dryRun() || aborted
        ));

        return new TacoImportReport(
            spreadsheet.rowsRead(), foods.size(), spreadsheet.issues().size(),
            result.inserted(), result.updated(), result.skipped(), spreadsheet.issues().size(),
            input.dryRun(), aborted, spreadsheet.issues()
        );
    }
}

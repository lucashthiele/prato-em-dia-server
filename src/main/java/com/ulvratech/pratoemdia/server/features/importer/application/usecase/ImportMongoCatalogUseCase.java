package com.ulvratech.pratoemdia.server.features.importer.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.*;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportMongoCatalog;

public class ImportMongoCatalogUseCase implements ImportMongoCatalog {
  public static final String SOURCE = "TBCA";
  public static final String SOURCE_VERSION = "TBCA scraper MongoDB contract v1";

  private final SyncImportedFoods catalog;

  public ImportMongoCatalogUseCase(SyncImportedFoods catalog) {
    this.catalog = catalog;
  }

  @Override
  public CatalogImportReport execute(ImportMongoCatalogInput input) {
    var data = input.data();
    boolean aborted = !input.dryRun() && !data.issues().isEmpty();
    var foods =
        data.records().stream()
            .map(
                record ->
                    new ImportedFoodInput(
                        record.externalId(),
                        record.name(),
                        record.category(),
                        record.brand(),
                        record.portions()))
            .toList();
    var result =
        catalog.execute(
            new SyncImportedFoodsInput(SOURCE, SOURCE_VERSION, foods, input.dryRun() || aborted));
    return new CatalogImportReport(
        data.documentsRead(),
        foods.size(),
        data.issues().size(),
        result.inserted(),
        result.updated(),
        result.skipped(),
        data.issues().size(),
        input.dryRun(),
        aborted,
        data.issues());
  }
}

package com.ulvratech.pratoemdia.server.features.importer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.CatalogImportIssue;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportTacoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoSpreadsheetData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportTacoCatalogUseCaseTest {
  @Test
  void invalidNonDryRunImportIsValidatedWithoutWritingToCatalog() {
    var catalog = mock(SyncImportedFoods.class);
    when(catalog.execute(any())).thenReturn(new SyncImportedFoodsOutput(0, 0, 0));
    var spreadsheet =
        new TacoSpreadsheetData(1, List.of(), List.of(new CatalogImportIssue(5, "invalid value")));

    var report =
        new ImportTacoCatalogUseCase(catalog)
            .execute(new ImportTacoCatalogInput(spreadsheet, false));

    assertTrue(report.aborted());
    verify(catalog).execute(argThat(SyncImportedFoodsInput::dryRun));
  }
}

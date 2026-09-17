package com.ulvratech.pratoemdia.server.features.importer.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.*;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportMongoCatalog;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportTacoCatalog;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportMongoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.MongoImportProperties;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.TacoImportProperties;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb.MongoCatalogImportRunner;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb.MongoCatalogReader;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter.ImportRunJpaRepository;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.model.ImportRunModel;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco.TacoCatalogImportRunner;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco.TacoSpreadsheetReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImportRunnersTest {
  @TempDir Path tempDir;

  private static CatalogImportReport report(boolean aborted) {
    return new CatalogImportReport(3, 3, 0, 1, 1, 1, aborted ? 1 : 0, false, aborted, List.of());
  }

  @Test
  void tacoRunnerRequiresAFile() {
    var runner =
        new TacoCatalogImportRunner(
            new TacoImportProperties(),
            mock(TacoSpreadsheetReader.class),
            mock(ImportTacoCatalog.class),
            mock(ImportRunJpaRepository.class));

    assertThrows(IllegalArgumentException.class, () -> runner.run(null));
  }

  @Test
  void tacoRunnerImportsAndRecordsSuccessfulAndAbortedRuns() throws Exception {
    var properties = new TacoImportProperties();
    Path file = tempDir.resolve("catalog.xls");
    Files.writeString(file, "catalog");
    properties.setFile(file);
    properties.setDryRun(false);
    var reader = mock(TacoSpreadsheetReader.class);
    var importer = mock(ImportTacoCatalog.class);
    var runs = mock(ImportRunJpaRepository.class);
    var data = new TacoSpreadsheetData(3, List.of(), List.of());
    when(reader.read(file)).thenReturn(data);
    when(importer.execute(any())).thenReturn(report(false));
    var runner = new TacoCatalogImportRunner(properties, reader, importer, runs);

    runner.run(null);

    var saved = org.mockito.ArgumentCaptor.forClass(ImportRunModel.class);
    verify(runs).save(saved.capture());
    assertEquals("COMPLETED", saved.getValue().getStatus());
    assertEquals(1, saved.getValue().getRowsInserted());
    assertEquals(64, saved.getValue().getSourceChecksum().length());

    when(importer.execute(any())).thenReturn(report(true));
    runner.run(null);
    verify(runs, times(2)).save(saved.capture());
    assertEquals("FAILED", saved.getValue().getStatus());
    assertEquals(0, saved.getValue().getRowsInserted());
  }

  @Test
  void tacoDryRunDoesNotPersistExecution() throws Exception {
    var properties = new TacoImportProperties();
    Path file = tempDir.resolve("catalog.xls");
    Files.writeString(file, "catalog");
    properties.setFile(file);
    var reader = mock(TacoSpreadsheetReader.class);
    when(reader.read(file)).thenReturn(new TacoSpreadsheetData(0, List.of(), List.of()));
    var importer = mock(ImportTacoCatalog.class);
    when(importer.execute(any())).thenReturn(report(false));
    var runs = mock(ImportRunJpaRepository.class);

    new TacoCatalogImportRunner(properties, reader, importer, runs).run(null);

    verify(runs, never()).save(any());
  }

  @Test
  void mongoRunnerPersistsResultAndSupportsDryRun() {
    var properties = new MongoImportProperties();
    properties.setDryRun(false);
    var reader = mock(MongoCatalogReader.class);
    var data = new MongoCatalogData(2, "checksum", List.of(), List.of());
    when(reader.read()).thenReturn(data);
    var importer = mock(ImportMongoCatalog.class);
    when(importer.execute(any())).thenReturn(report(false));
    var runs = mock(ImportRunJpaRepository.class);
    var runner = new MongoCatalogImportRunner(properties, reader, importer, runs);

    runner.run(null);

    var saved = org.mockito.ArgumentCaptor.forClass(ImportRunModel.class);
    verify(runs).save(saved.capture());
    assertEquals("COMPLETED", saved.getValue().getStatus());
    assertEquals("tbca.foods", saved.getValue().getSourceLocation());

    properties.setDryRun(true);
    runner.run(null);
    verify(runs, times(1)).save(any());
  }

  @Test
  void mongoRunnerRecordsAnAbortedImport() {
    var properties = new MongoImportProperties();
    properties.setDryRun(false);
    var reader = mock(MongoCatalogReader.class);
    when(reader.read()).thenReturn(new MongoCatalogData(1, "sum", List.of(), List.of()));
    var importer = mock(ImportMongoCatalog.class);
    when(importer.execute(any())).thenReturn(report(true));
    var runs = mock(ImportRunJpaRepository.class);

    new MongoCatalogImportRunner(properties, reader, importer, runs).run(null);

    var saved = org.mockito.ArgumentCaptor.forClass(ImportRunModel.class);
    verify(runs).save(saved.capture());
    assertEquals("FAILED", saved.getValue().getStatus());
    assertEquals(0, saved.getValue().getRowsUpdated());
  }

  @Test
  void mongoUseCaseMapsRecordsAndAbortsInvalidWrites() {
    var catalog = mock(SyncImportedFoods.class);
    when(catalog.execute(any())).thenReturn(new SyncImportedFoodsOutput(1, 2, 3));
    var useCase = new ImportMongoCatalogUseCase(catalog);
    var record = new MongoFoodRecord("id", "name", "brand", "category", List.of());
    var issue = new CatalogImportIssue(1, "invalid");

    var output =
        useCase.execute(
            new ImportMongoCatalogInput(
                new MongoCatalogData(2, "sum", List.of(record), List.of(issue)), false));

    assertTrue(output.aborted());
    assertEquals(1, output.valid());
    var input =
        org.mockito.ArgumentCaptor.forClass(
            com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput
                .class);
    verify(catalog).execute(input.capture());
    assertTrue(input.getValue().dryRun());
    assertEquals("TBCA", input.getValue().source());
  }

  @Test
  void catalogReportFormatsEveryModeAndCopiesIssues() {
    var issues = new java.util.ArrayList<CatalogImportIssue>();
    issues.add(new CatalogImportIssue(7, "bad row"));
    var dry = new CatalogImportReport(3, 2, 1, 1, 1, 0, 1, true, false, issues);
    issues.clear();
    assertTrue(dry.formatted().contains("Dry-run"));
    assertTrue(dry.formatted().contains("bad row"));
    assertEquals(1, dry.toInsert());
    assertEquals(1, dry.toUpdate());
    assertEquals(0, dry.toSkip());
    assertTrue(report(true).formatted().contains("Import aborted"));
    assertTrue(report(false).formatted().contains("Inserted"));
  }

  @Test
  void importPropertiesExposeConfiguredValues() {
    var mongo = new MongoImportProperties();
    mongo.setUri("mongodb://test");
    mongo.setDatabase("db");
    mongo.setCollection("foods2");
    assertEquals("mongodb://test", mongo.getUri());
    assertEquals("db", mongo.getDatabase());
    assertEquals("foods2", mongo.getCollection());
    var taco = new TacoImportProperties();
    taco.setFile(tempDir);
    taco.setDryRun(false);
    assertEquals(tempDir, taco.getFile());
    assertFalse(taco.isDryRun());
  }
}

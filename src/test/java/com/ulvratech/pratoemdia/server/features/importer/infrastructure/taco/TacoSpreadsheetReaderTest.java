package com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TacoSpreadsheetReaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void readsAValidFoodAndPreservesSpecialNutrientValues() throws Exception {
    Path file = temporaryDirectory.resolve("taco.xls");
    try (var workbook = new HSSFWorkbook()) {
      var sheet = workbook.createSheet("composition");
      sheet.createRow(0);
      var labels = sheet.createRow(1);
      labels.createCell(3).setCellValue("Energia");
      labels.createCell(5).setCellValue("Proteína");
      labels.createCell(17).setCellValue("Sódio");
      var headers = sheet.createRow(2);
      headers.createCell(0).setCellValue("Alimento");
      headers.createCell(1).setCellValue("Descrição dos alimentos");
      var group = sheet.createRow(3);
      group.createCell(0).setCellValue("Cereais e derivados");
      var food = sheet.createRow(4);
      food.createCell(0).setCellValue(1);
      food.createCell(1).setCellValue("Arroz, integral, cozido");
      food.createCell(3).setCellValue(123.5);
      food.createCell(5).setCellValue("Tr");
      food.createCell(8).setCellValue(-0.0266666666666662);
      food.createCell(17).setCellValue("NA");
      try (OutputStream output = Files.newOutputStream(file)) {
        workbook.write(output);
      }
    }

    var data = new TacoSpreadsheetReader(new TacoNutrientValueParser()).read(file);

    assertEquals(1, data.rowsRead());
    assertEquals(1, data.records().size());
    assertEquals("1", data.records().getFirst().externalId());
    assertEquals("Cereais e derivados", data.records().getFirst().category());
    assertEquals(
        "123.5", data.records().getFirst().nutrients().get("energy_kcal").amount().toPlainString());
    assertEquals(
        NutrientValueStatus.TRACE, data.records().getFirst().nutrients().get("protein").status());
    assertEquals(
        "0", data.records().getFirst().nutrients().get("carbohydrate").amount().toPlainString());
    assertEquals(
        "-0.0266666666666662",
        data.records().getFirst().nutrients().get("carbohydrate").rawValue());
    assertEquals(
        NutrientValueStatus.NOT_APPLICABLE,
        data.records().getFirst().nutrients().get("sodium").status());
    assertEquals(
        NutrientValueStatus.NOT_AVAILABLE,
        data.records().getFirst().nutrients().get("lipids").status());
  }
}

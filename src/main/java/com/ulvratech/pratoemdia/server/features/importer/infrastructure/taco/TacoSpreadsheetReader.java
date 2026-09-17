package com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.CatalogImportIssue;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoFoodRecord;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoSpreadsheetData;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

public class TacoSpreadsheetReader {
  private static final BigDecimal NEGATIVE_CARBOHYDRATE_ROUNDING_LIMIT = new BigDecimal("-0.1");

  private static final List<NutrientColumn> NUTRIENTS =
      List.of(
          new NutrientColumn(2, "humidity", "%"),
          new NutrientColumn(3, "energy_kcal", "kcal"),
          new NutrientColumn(4, "energy_kj", "kJ"),
          new NutrientColumn(5, "protein", "g"),
          new NutrientColumn(6, "lipids", "g"),
          new NutrientColumn(7, "cholesterol", "mg"),
          new NutrientColumn(8, "carbohydrate", "g"),
          new NutrientColumn(9, "dietary_fiber", "g"),
          new NutrientColumn(10, "ashes", "g"),
          new NutrientColumn(11, "calcium", "mg"),
          new NutrientColumn(12, "magnesium", "mg"),
          new NutrientColumn(14, "manganese", "mg"),
          new NutrientColumn(15, "phosphorus", "mg"),
          new NutrientColumn(16, "iron", "mg"),
          new NutrientColumn(17, "sodium", "mg"),
          new NutrientColumn(18, "potassium", "mg"),
          new NutrientColumn(19, "copper", "mg"),
          new NutrientColumn(20, "zinc", "mg"),
          new NutrientColumn(21, "retinol", "mcg"),
          new NutrientColumn(22, "vitamin_a_re", "mcg"),
          new NutrientColumn(23, "vitamin_a_rae", "mcg"),
          new NutrientColumn(24, "thiamine", "mg"),
          new NutrientColumn(25, "riboflavin", "mg"),
          new NutrientColumn(26, "pyridoxine", "mg"),
          new NutrientColumn(27, "niacin", "mg"),
          new NutrientColumn(28, "vitamin_c", "mg"));

  private final TacoNutrientValueParser valueParser;

  public TacoSpreadsheetReader(TacoNutrientValueParser valueParser) {
    this.valueParser = valueParser;
  }

  public TacoSpreadsheetData read(Path path) throws IOException {
    if (path == null) throw new IllegalArgumentException("catalog.import.file is required");
    if (!Files.isRegularFile(path))
      throw new IllegalArgumentException("TACO file not found: " + path);
    try (InputStream input = Files.newInputStream(path);
        var workbook = new HSSFWorkbook(input)) {
      Sheet sheet = findCompositionSheet(workbook);
      validateLayout(sheet);
      return readRows(sheet);
    }
  }

  private Sheet findCompositionSheet(Workbook workbook) {
    for (Sheet sheet : workbook) {
      if (text(sheet.getRow(2), 0).equalsIgnoreCase("Alimento")
          && text(sheet.getRow(2), 1).startsWith("Descrição")) {
        return sheet;
      }
    }
    throw new IllegalArgumentException("TACO composition sheet was not found");
  }

  private void validateLayout(Sheet sheet) {
    if (!text(sheet.getRow(1), 3).equalsIgnoreCase("Energia")
        || !text(sheet.getRow(1), 5).equalsIgnoreCase("Proteína")
        || !text(sheet.getRow(1), 17).equalsIgnoreCase("Sódio")) {
      throw new IllegalArgumentException("Unsupported TACO spreadsheet layout");
    }
  }

  private TacoSpreadsheetData readRows(Sheet sheet) {
    var records = new ArrayList<TacoFoodRecord>();
    var issues = new ArrayList<CatalogImportIssue>();
    var externalIds = new HashSet<String>();
    String category = null;
    int rowsRead = 0;

    for (int index = 3; index <= sheet.getLastRowNum(); index++) {
      Row row = sheet.getRow(index);
      if (row == null) continue;
      Cell first = row.getCell(0);
      if (first == null || first.getCellType() == CellType.BLANK) continue;

      CellType firstType = effectiveType(first);
      if (firstType == CellType.STRING) {
        String candidate = first.getStringCellValue().trim();
        if (isCategoryRow(row, candidate)) category = candidate;
        continue;
      }
      if (firstType != CellType.NUMERIC) continue;

      rowsRead++;
      int spreadsheetRow = index + 1;
      try {
        double numericId = first.getNumericCellValue();
        if (numericId != Math.rint(numericId)) {
          throw new IllegalArgumentException("food id must be an integer");
        }
        String externalId = Long.toString((long) numericId);
        String description = text(row, 1);
        if (description.isBlank()) throw new IllegalArgumentException("description is required");
        if (category == null) throw new IllegalArgumentException("category is required");
        if (!externalIds.add(externalId)) {
          throw new IllegalArgumentException("duplicate TACO id " + externalId);
        }
        var nutrients = new LinkedHashMap<String, NutrientValue>();
        for (var nutrient : NUTRIENTS) {
          nutrients.put(nutrient.key(), parseNutrient(row, nutrient));
        }
        records.add(new TacoFoodRecord(externalId, description, category, nutrients));
      } catch (IllegalArgumentException exception) {
        issues.add(new CatalogImportIssue(spreadsheetRow, exception.getMessage()));
      }
    }
    return new TacoSpreadsheetData(rowsRead, records, issues);
  }

  private NutrientValue parseNutrient(Row row, NutrientColumn nutrient) {
    NutrientValue value =
        valueParser.parse(rawValue(row.getCell(nutrient.index())), nutrient.unit());
    if (value.amount() == null || value.amount().signum() >= 0) return value;

    if (nutrient.key().equals("carbohydrate")
        && value.amount().compareTo(NEGATIVE_CARBOHYDRATE_ROUNDING_LIMIT) >= 0) {
      return new NutrientValue(BigDecimal.ZERO, value.unit(), value.status(), value.rawValue());
    }

    throw new IllegalArgumentException(
        "negative nutrient value for " + nutrient.key() + ": " + value.rawValue());
  }

  private boolean isCategoryRow(Row row, String value) {
    if (!text(row, 1).isBlank()) return false;
    return !value.equalsIgnoreCase("Número do")
        && !value.equalsIgnoreCase("Alimento")
        && !value.equalsIgnoreCase("Legenda")
        && !value.startsWith("*")
        && !value.startsWith("†");
  }

  private Object rawValue(Cell cell) {
    if (cell == null) return null;
    return switch (cell.getCellType()) {
      case NUMERIC -> Double.toString(cell.getNumericCellValue());
      case STRING -> cell.getStringCellValue();
      case BLANK -> null;
      case FORMULA ->
          switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> Double.toString(cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue();
            case BLANK -> null;
            default -> throw new IllegalArgumentException("unsupported formula result");
          };
      default -> cell.toString();
    };
  }

  private String text(Row row, int index) {
    if (row == null || row.getCell(index) == null) return "";
    Cell cell = row.getCell(index);
    CellType type = effectiveType(cell);
    if (type == CellType.STRING) return cell.getStringCellValue().trim();
    if (type == CellType.NUMERIC) return Double.toString(cell.getNumericCellValue());
    return cell.toString().trim();
  }

  private CellType effectiveType(Cell cell) {
    return cell.getCellType() == CellType.FORMULA
        ? cell.getCachedFormulaResultType()
        : cell.getCellType();
  }

  private record NutrientColumn(int index, String key, String unit) {}
}

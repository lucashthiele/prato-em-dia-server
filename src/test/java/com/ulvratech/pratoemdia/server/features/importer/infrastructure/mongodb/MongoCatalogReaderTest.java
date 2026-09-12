package com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.MongoImportProperties;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.bson.types.Decimal128;

import static org.junit.jupiter.api.Assertions.*;

class MongoCatalogReaderTest {
    private final MongoCatalogReader reader = new MongoCatalogReader(new MongoImportProperties());

    @Test
    void mapsTheScraperContractIncludingNotAnalyzedAndUnknownPortionUnit() {
        var nutrient = new Document()
            .append("amount", null)
            .append("unit", "mg")
            .append("status", "NOT_ANALYZED")
            .append("rawValue", "-;");
        var standard = new Document()
            .append("type", "STANDARD")
            .append("label", "100 g")
            .append("rawLabel", "Valor por 100g")
            .append("quantity", 100)
            .append("unit", "g")
            .append("nutrients", new Document("sodium", nutrient));
        var household = new Document()
            .append("type", "HOUSEHOLD")
            .append("label", "Colher sopa cheia")
            .append("rawLabel", "Colher sopa cheia (9 )")
            .append("quantity", 9)
            .append("unit", null)
            .append("nutrients", new Document("sodium", nutrient));
        var document = new Document()
            .append("externalId", "BRC1221A")
            .append("name", "Araruta, farinha, crua")
            .append("brand", null)
            .append("category", "Cereais e derivados")
            .append("portions", List.of(standard, household));

        var result = reader.toRecord(document);

        assertEquals("BRC1221A", result.externalId());
        assertEquals(2, result.portions().size());
        assertNull(result.portions().get(1).unit());
        assertEquals(
            NutrientValueStatus.NOT_ANALYZED,
            result.portions().getFirst().nutrients().get("sodium").status()
        );
    }

    @Test
    void rejectsDocumentsWithoutTheStandardPortion() {
        var document = new Document()
            .append("externalId", "1")
            .append("name", "Food")
            .append("category", "Category")
            .append("portions", List.of(new Document()
                .append("type", "HOUSEHOLD")
                .append("label", "Spoon")
                .append("quantity", 10)
                .append("unit", "g")
                .append("nutrients", new Document())));

        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(document));
    }

    @Test
    void requiresMongoUriBeforeConnecting() {
        assertThrows(IllegalArgumentException.class, reader::read);
    }

    @Test
    void rejectsMalformedCatalogRecordsWithContext() {
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(new Document()));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of())));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of("invalid"))));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of(
            portion("OTHER", "100 g", 100, "g", nutrients(1))))));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of(
            portion("STANDARD", "100 g", 0, "g", nutrients(1))))));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of(
            portion("STANDARD", "100 g", 100, "g", new Document())))));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of(
            portion("STANDARD", "100 g", 90, "g", nutrients(1))))));
        var duplicate = portion("STANDARD", "100 g", 100, "g", nutrients(1));
        assertThrows(IllegalArgumentException.class,
            () -> reader.toRecord(base(List.of(duplicate, duplicate))));
    }

    @Test
    void acceptsMapDocumentsAndSupportedDecimalRepresentations() {
        var nutrientMap = Map.of("amount", "1,5", "unit", "mg", "status", "VALUE");
        var nutrients = new Document("first", nutrientMap)
            .append("second", new Document("amount", new Decimal128(new BigDecimal("2.0")))
                .append("unit", "mg").append("status", "VALUE"));
        var result = reader.toRecord(base(List.of(
            portion("STANDARD", "100 g", new BigDecimal("100.0"), "g", nutrients))));

        assertEquals(new BigDecimal("1.5"), result.portions().getFirst().nutrients().get("first").amount());
        assertEquals(new BigDecimal("2"), result.portions().getFirst().nutrients().get("second").amount());
    }

    @Test
    void rejectsInvalidNutrientValues() {
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of(
            portion("STANDARD", "100 g", 100, "g", nutrients(-1))))));
        var invalid = new Document("x", new Document("amount", "not-a-number")
            .append("unit", "mg").append("status", "VALUE"));
        assertThrows(IllegalArgumentException.class, () -> reader.toRecord(base(List.of(
            portion("STANDARD", "100 g", 100, "g", invalid)))));
    }

    private Document base(List<?> portions) {
        return new Document("externalId", " id ").append("name", " Food ")
            .append("brand", " ").append("category", " Category ").append("portions", portions);
    }

    private Document portion(String type, String label, Object quantity, String unit, Document nutrients) {
        return new Document("type", type).append("label", label).append("quantity", quantity)
            .append("unit", unit).append("nutrients", nutrients);
    }

    private Document nutrients(int amount) {
        return new Document("energy", new Document("amount", amount).append("unit", "kcal")
            .append("status", "VALUE").append("rawValue", String.valueOf(amount)));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
    void readsTheConfiguredMongoCatalog() {
        var properties = new MongoImportProperties();
        properties.setUri(System.getenv("MONGODB_URI"));
        properties.setDatabase(System.getenv().getOrDefault("MONGODB_DATABASE", "tbca"));
        properties.setCollection(System.getenv().getOrDefault("MONGODB_COLLECTION", "foods"));

        var data = new MongoCatalogReader(properties).read();

        assertTrue(data.documentsRead() > 0);
        assertEquals(data.documentsRead(), data.records().size());
        assertTrue(data.issues().isEmpty(), () -> data.issues().toString());
        assertEquals(64, data.checksum().length());
        assertTrue(data.records().stream().allMatch(record -> record.portions().stream()
            .anyMatch(portion -> "STANDARD".equals(portion.type()))));
    }
}

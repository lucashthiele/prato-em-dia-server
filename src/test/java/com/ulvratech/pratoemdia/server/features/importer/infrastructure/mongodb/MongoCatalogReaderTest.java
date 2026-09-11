package com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.MongoImportProperties;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

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

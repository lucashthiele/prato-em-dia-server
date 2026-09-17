package com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb;

import com.mongodb.client.MongoClients;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodPortionInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.CatalogImportIssue;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.MongoCatalogData;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.MongoFoodRecord;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.MongoImportProperties;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MongoCatalogReader {
    private final MongoImportProperties properties;

    public MongoCatalogReader(MongoImportProperties properties) {
        this.properties = properties;
    }

    public MongoCatalogData read() {
        if (properties.getUri() == null || properties.getUri().isBlank()) {
            throw new IllegalArgumentException("catalog.import.mongodb.uri is required");
        }
        var records = new ArrayList<MongoFoodRecord>();
        var issues = new ArrayList<CatalogImportIssue>();
        var externalIds = new HashSet<String>();
        var digest = sha256();
        int read = 0;

        try (var client = MongoClients.create(properties.getUri())) {
            var collection = client.getDatabase(properties.getDatabase())
                    .getCollection(properties.getCollection());
            for (var document : collection.find().sort(new Document("externalId", 1))) {
                read++;
                digest.update(document.toJson().getBytes(StandardCharsets.UTF_8));
                try {
                    var record = toRecord(document);
                    if (!externalIds.add(record.externalId())) {
                        throw new IllegalArgumentException(
                                "duplicate externalId " + record.externalId()
                        );
                    }
                    records.add(record);
                } catch (IllegalArgumentException exception) {
                    issues.add(new CatalogImportIssue(read, exception.getMessage()));
                }
            }
        }
        return new MongoCatalogData(read, HexFormat.of().formatHex(digest.digest()), records, issues);
    }

    MongoFoodRecord toRecord(Document document) {
        String externalId = requiredString(document, "externalId");
        String name = requiredString(document, "name");
        String brand = optionalString(document, "brand");
        String category = requiredString(document, "category");
        List<?> rawPortions = document.getList("portions", Object.class);
        if (rawPortions == null || rawPortions.isEmpty()) {
            throw new IllegalArgumentException(externalId + ": portions are required");
        }
        var portions = rawPortions.stream()
                .map(value -> asDocument(value, externalId + ": invalid portion"))
                .map(portion -> toPortion(externalId, portion))
                .toList();
        long distinctPortions = portions.stream().map(
                portion -> List.of(
                        portion.type(), portion.label(), portion.quantity(),
                        Objects.toString(portion.unit(), "")
                )
        ).distinct().count();
        if (distinctPortions != portions.size()) {
            throw new IllegalArgumentException(externalId + ": duplicate portion identity");
        }
        long standard = portions.stream().filter(portion -> "STANDARD".equals(portion.type())).count();
        if (standard != 1) {
            throw new IllegalArgumentException(externalId + ": exactly one STANDARD portion is required");
        }
        boolean validStandard = portions.stream().filter(portion -> "STANDARD".equals(portion.type()))
                .allMatch(
                        portion -> portion.quantity().compareTo(new BigDecimal("100")) == 0
                                && "g".equals(portion.unit())
                );
        if (!validStandard) {
            throw new IllegalArgumentException(externalId + ": STANDARD portion must be 100 g");
        }
        return new MongoFoodRecord(externalId, name, brand, category, portions);
    }

    private ImportedFoodPortionInput toPortion(String externalId, Document portion) {
        String type = requiredString(portion, "type");
        if (!Set.of("STANDARD", "HOUSEHOLD").contains(type)) {
            throw new IllegalArgumentException(externalId + ": unsupported portion type " + type);
        }
        String label = requiredString(portion, "label");
        String rawLabel = optionalString(portion, "rawLabel");
        BigDecimal quantity = decimal(portion.get("quantity"));
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException(externalId + "/" + label + ": positive quantity is required");
        }
        String unit = optionalString(portion, "unit");
        var nutrientDocument = asDocument(
                portion.get("nutrients"), externalId + "/" + label + ": nutrients are required"
        );
        if (nutrientDocument.isEmpty()) {
            throw new IllegalArgumentException(externalId + "/" + label + ": nutrients are required");
        }
        var nutrients = new LinkedHashMap<String, NutrientValue>();
        nutrientDocument.forEach(
                (code, value) -> nutrients.put(
                        code, toNutrient(externalId, label, code, asDocument(value, "invalid nutrient " + code))
                )
        );
        return new ImportedFoodPortionInput(
                type, label, rawLabel, quantity.stripTrailingZeros(), unit, nutrients
        );
    }

    private NutrientValue toNutrient(
            String externalId, String label, String code, Document nutrient
    ) {
        try {
            var status = NutrientValueStatus.valueOf(requiredString(nutrient, "status"));
            BigDecimal amount = decimal(nutrient.get("amount"));
            if (amount != null && amount.signum() < 0) {
                throw new IllegalArgumentException("amount must not be negative");
            }
            if (amount != null)
                amount = amount.stripTrailingZeros();
            return new NutrientValue(
                    amount, requiredString(nutrient, "unit"), status,
                    optionalString(nutrient, "rawValue")
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    externalId + "/" + label + "/" + code + ": " + exception.getMessage(), exception
            );
        }
    }

    private Document asDocument(Object value, String message) {
        if (value instanceof Document document)
            return document;
        if (value instanceof Map<?, ?> map) {
            var document = new Document();
            map.forEach((key, item) -> document.put(String.valueOf(key), item));
            return document;
        }
        throw new IllegalArgumentException(message);
    }

    private String requiredString(Document document, String key) {
        String value = optionalString(document, key);
        if (value == null)
            throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private String optionalString(Document document, String key) {
        Object value = document.get(key);
        if (value == null || value.toString().isBlank())
            return null;
        return value.toString().trim();
    }

    private BigDecimal decimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof Decimal128 decimal)
            return decimal.bigDecimalValue();
        if (value instanceof BigDecimal decimal)
            return decimal;
        if (value instanceof Number number)
            return new BigDecimal(number.toString());
        try {
            return new BigDecimal(value.toString().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid decimal value " + value, exception);
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

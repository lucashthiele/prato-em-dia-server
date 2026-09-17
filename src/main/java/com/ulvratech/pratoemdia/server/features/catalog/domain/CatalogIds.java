package com.ulvratech.pratoemdia.server.features.catalog.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.util.UUID;

public final class CatalogIds {
    public static final UUID PRATO_EM_DIA_CATALOG_NAMESPACE = UUID.fromString("d47d0e7a-3c88-5f19-9a6e-6b9f6b0c58be");

    private CatalogIds() {
    }

    public static UUID tacoFood(String externalId) {
        return importedFood("TACO", externalId);
    }

    public static UUID importedFood(String source, String externalId) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
        return uuidV5(
                PRATO_EM_DIA_CATALOG_NAMESPACE,
                source.trim().toUpperCase() + ":FOOD:" + externalId.trim()
        );
    }

    public static UUID importedFoodPortion(
            UUID foodId, String type, String label, BigDecimal quantity, String unit
    ) {
        if (foodId == null)
            throw new IllegalArgumentException("foodId is required");
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("type is required");
        if (label == null || label.isBlank())
            throw new IllegalArgumentException("label is required");
        if (quantity == null)
            throw new IllegalArgumentException("quantity is required");
        return uuidV5(
                PRATO_EM_DIA_CATALOG_NAMESPACE,
                "FOOD:PORTION:" + foodId + ":" + type.trim() + ":" + label.trim()
                        + ":" + quantity.stripTrailingZeros().toPlainString() + ":"
                        + (unit == null ? "" : unit.trim())
        );
    }

    public static UUID foodPortionNutrient(UUID portionId, String nutrientCode) {
        if (portionId == null)
            throw new IllegalArgumentException("portionId is required");
        if (nutrientCode == null || nutrientCode.isBlank()) {
            throw new IllegalArgumentException("nutrientCode is required");
        }
        return uuidV5(
                PRATO_EM_DIA_CATALOG_NAMESPACE,
                "FOOD:PORTION:NUTRIENT:" + portionId + ":" + nutrientCode.trim()
        );
    }

    static UUID uuidV5(UUID namespace, String name) {
        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(toBytes(namespace));
            byte[] hash = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            var bytes = ByteBuffer.wrap(hash);
            return new UUID(bytes.getLong(), bytes.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required by UUID v5", exception);
        }
    }

    private static byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }
}

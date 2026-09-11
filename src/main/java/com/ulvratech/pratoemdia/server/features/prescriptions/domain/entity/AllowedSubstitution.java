package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AllowedSubstitution(
    UUID id, UUID foodId, UUID foodPortionId, BigDecimal quantity, String unit,
    String notes, Instant createdAt
) {
    public AllowedSubstitution {
        if (id == null || foodId == null) throw new IllegalArgumentException("Substitution identifiers are required");
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("Substitution quantity must be positive");
        unit = required(unit, "Substitution unit is required", 32);
        notes = normalized(notes);
    }

    public static AllowedSubstitution create(UUID foodId, UUID portionId, BigDecimal quantity, String unit, String notes) {
        return new AllowedSubstitution(UUID.randomUUID(), foodId, portionId, quantity, unit, notes, null);
    }

    private static String required(String value, String message, int max) {
        var normalized = normalized(value);
        if (normalized == null || normalized.length() > max) throw new IllegalArgumentException(message);
        return normalized;
    }

    static String normalized(String value) {
        if (value == null) return null;
        var normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

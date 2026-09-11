package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class FoodPortion {
    private final UUID id;
    private final UUID foodId;
    private final String label;
    private final BigDecimal quantity;
    private final String unit;
    private final BigDecimal gramsEquivalent;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String sourceType;
    private final String rawLabel;
    private final Map<String, NutrientValue> nutrients;

    public FoodPortion(
        UUID id, UUID foodId, String label, BigDecimal quantity, String unit,
        BigDecimal gramsEquivalent, boolean active, Instant createdAt, Instant updatedAt
    ) {
        this(id, foodId, label, quantity, unit, gramsEquivalent, active, createdAt,
            updatedAt, null, null, Map.of());
    }

    public FoodPortion(
        UUID id, UUID foodId, String label, BigDecimal quantity, String unit,
        BigDecimal gramsEquivalent, boolean active, Instant createdAt, Instant updatedAt,
        String sourceType, String rawLabel, Map<String, NutrientValue> nutrients
    ) {
        if (foodId == null) throw new IllegalArgumentException("foodId is required");
        if (label == null || label.isBlank()) throw new IllegalArgumentException("label is required");
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if ((unit == null || unit.isBlank()) && sourceType == null) {
            throw new IllegalArgumentException("unit is required");
        }
        if (gramsEquivalent != null && gramsEquivalent.signum() <= 0) {
            throw new IllegalArgumentException("gramsEquivalent must be positive");
        }
        this.id = id;
        this.foodId = foodId;
        this.label = label.trim();
        this.quantity = quantity.stripTrailingZeros();
        this.unit = optional(unit);
        this.gramsEquivalent = gramsEquivalent == null ? null : gramsEquivalent.stripTrailingZeros();
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sourceType = optional(sourceType);
        this.rawLabel = optional(rawLabel);
        this.nutrients = nutrients == null
            ? Map.of()
            : Map.copyOf(new LinkedHashMap<>(nutrients));
    }

    public static FoodPortion create(
        UUID foodId, String label, BigDecimal quantity, String unit, BigDecimal gramsEquivalent
    ) {
        return new FoodPortion(
            UUID.randomUUID(), foodId, label, quantity, unit, gramsEquivalent, true, null, null
        );
    }

    public static FoodPortion imported(
        UUID id, UUID foodId, String sourceType, String label, String rawLabel,
        BigDecimal quantity, String unit, Map<String, NutrientValue> nutrients
    ) {
        BigDecimal gramsEquivalent = "g".equals(unit) ? quantity : null;
        return new FoodPortion(
            id, foodId, label, quantity, unit, gramsEquivalent, true, null, null,
            sourceType, rawLabel, nutrients
        );
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getFoodId() { return foodId; }
    public String getLabel() { return label; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getGramsEquivalent() { return gramsEquivalent; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getSourceType() { return sourceType; }
    public String getRawLabel() { return rawLabel; }
    public Map<String, NutrientValue> getNutrients() { return nutrients; }
}

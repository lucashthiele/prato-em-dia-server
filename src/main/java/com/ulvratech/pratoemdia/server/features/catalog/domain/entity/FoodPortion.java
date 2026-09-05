package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
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

    public FoodPortion(
        UUID id,
        UUID foodId,
        String label,
        BigDecimal quantity,
        String unit,
        BigDecimal gramsEquivalent,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {
        if (foodId == null) throw new IllegalArgumentException("foodId is required");
        if (label == null || label.isBlank()) throw new IllegalArgumentException("label is required");
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (unit == null || unit.isBlank()) throw new IllegalArgumentException("unit is required");
        if (gramsEquivalent != null && gramsEquivalent.signum() <= 0) {
            throw new IllegalArgumentException("gramsEquivalent must be positive");
        }
        this.id = id;
        this.foodId = foodId;
        this.label = label.trim();
        this.quantity = quantity;
        this.unit = unit.trim();
        this.gramsEquivalent = gramsEquivalent;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FoodPortion create(
        UUID foodId, String label, BigDecimal quantity, String unit, BigDecimal gramsEquivalent
    ) {
        return new FoodPortion(
            UUID.randomUUID(), foodId, label, quantity, unit, gramsEquivalent, true, null, null
        );
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
}

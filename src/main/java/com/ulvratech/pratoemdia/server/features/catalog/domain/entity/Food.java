package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;

import java.time.Instant;
import java.util.UUID;

public class Food {
    private final UUID id;
    private final UUID createdByUserId;
    private final FoodOrigin origin;
    private final Instant createdAt;
    private final long version;
    private String name;
    private String category;
    private String defaultUnit;
    private String notes;
    private String preparationMethod;
    private boolean active;
    private Instant updatedAt;

    public Food(
        UUID id,
        String name,
        String category,
        String defaultUnit,
        String notes,
        String preparationMethod,
        boolean active,
        UUID createdByUserId,
        FoodOrigin origin,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
        this.id = id;
        this.name = required(name, "name");
        this.category = optional(category);
        this.defaultUnit = required(defaultUnit, "defaultUnit");
        this.notes = optional(notes);
        this.preparationMethod = optional(preparationMethod);
        this.active = active;
        this.createdByUserId = createdByUserId;
        if (origin == null) throw new IllegalArgumentException("origin is required");
        this.origin = origin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Food create(
        String name,
        String category,
        String defaultUnit,
        String notes,
        String preparationMethod,
        UUID createdByUserId,
        FoodOrigin origin
    ) {
        if (createdByUserId == null) throw new IllegalArgumentException("createdByUserId is required");
        return new Food(
            UUID.randomUUID(), name, category, defaultUnit, notes, preparationMethod,
            true, createdByUserId, origin, null, null, 0
        );
    }

    public void update(
        String name,
        String category,
        String defaultUnit,
        String notes,
        String preparationMethod,
        boolean active
    ) {
        this.name = required(name, "name");
        this.category = optional(category);
        this.defaultUnit = required(defaultUnit, "defaultUnit");
        this.notes = optional(notes);
        this.preparationMethod = optional(preparationMethod);
        this.active = active;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDefaultUnit() { return defaultUnit; }
    public String getNotes() { return notes; }
    public String getPreparationMethod() { return preparationMethod; }
    public boolean isActive() { return active; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public FoodOrigin getOrigin() { return origin; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

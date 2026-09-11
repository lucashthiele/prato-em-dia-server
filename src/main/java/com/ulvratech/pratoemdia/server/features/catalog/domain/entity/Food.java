package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Food {
    private final UUID id;
    private final UUID createdByUserId;
    private final FoodOrigin origin;
    private final Instant createdAt;
    private final long version;
    private final String source;
    private final String sourceExternalId;
    private String name;
    private String category;
    private String defaultUnit;
    private String notes;
    private String preparationMethod;
    private boolean active;
    private Instant updatedAt;
    private String sourceDescription;
    private String sourceVersion;
    private String normalizedName;
    private Map<String, NutrientValue> nutrients;

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
        this(
            id, name, category, defaultUnit, notes, preparationMethod, active,
            createdByUserId, origin, createdAt, updatedAt, version,
            null, null, null, null, null, Map.of()
        );
    }

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
        long version,
        String source,
        String sourceExternalId,
        String sourceDescription,
        String sourceVersion,
        String normalizedName,
        Map<String, NutrientValue> nutrients
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
        this.source = optional(source);
        this.sourceExternalId = optional(sourceExternalId);
        this.sourceDescription = optional(sourceDescription);
        this.sourceVersion = optional(sourceVersion);
        this.normalizedName = optional(normalizedName);
        this.nutrients = immutableNutrients(nutrients);
        if ((this.source == null) != (this.sourceExternalId == null)
            || (this.source == null) != (this.sourceDescription == null)) {
            throw new IllegalArgumentException("source identity must be complete");
        }
        if (createdByUserId == null && this.source == null) {
            throw new IllegalArgumentException("createdByUserId is required for non-imported food");
        }
    }

    public static Food imported(
        UUID id,
        String name,
        String category,
        String source,
        String sourceExternalId,
        String sourceDescription,
        String sourceVersion,
        String normalizedName,
        Map<String, NutrientValue> nutrients
    ) {
        return new Food(
            id, name, category, "100 g", null, null, true, null,
            FoodOrigin.IMPORTED, null, null, 0, source, sourceExternalId,
            sourceDescription, sourceVersion, normalizedName, nutrients
        );
    }

    public boolean hasImportedContent(
        String name,
        String category,
        String sourceDescription,
        String sourceVersion,
        String normalizedName,
        Map<String, NutrientValue> nutrients
    ) {
        return Objects.equals(this.name, required(name, "name"))
            && Objects.equals(this.category, optional(category))
            && Objects.equals(this.sourceDescription, optional(sourceDescription))
            && Objects.equals(this.sourceVersion, optional(sourceVersion))
            && Objects.equals(this.normalizedName, optional(normalizedName))
            && Objects.equals(this.nutrients, immutableNutrients(nutrients))
            && active;
    }

    public void updateImported(
        String name,
        String category,
        String sourceDescription,
        String sourceVersion,
        String normalizedName,
        Map<String, NutrientValue> nutrients
    ) {
        if (source == null) throw new IllegalStateException("food is not imported");
        this.name = required(name, "name");
        this.category = optional(category);
        this.sourceDescription = required(sourceDescription, "sourceDescription");
        this.sourceVersion = optional(sourceVersion);
        this.normalizedName = optional(normalizedName);
        this.nutrients = immutableNutrients(nutrients);
        this.active = true;
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

    private static Map<String, NutrientValue> immutableNutrients(
        Map<String, NutrientValue> nutrients
    ) {
        return nutrients == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(nutrients));
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
    public String getSource() { return source; }
    public String getSourceExternalId() { return sourceExternalId; }
    public String getSourceDescription() { return sourceDescription; }
    public String getSourceVersion() { return sourceVersion; }
    public String getNormalizedName() { return normalizedName; }
    public Map<String, NutrientValue> getNutrients() { return nutrients; }
}

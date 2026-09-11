package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PrescribedMeal(
    UUID id, String name, int order, LocalTime suggestedTime, String notes,
    Instant createdAt, List<PrescribedMealItem> items
) {
    public PrescribedMeal {
        if (id == null) throw new IllegalArgumentException("Meal id is required");
        name = AllowedSubstitution.normalized(name);
        if (name == null || name.length() > 120) throw new IllegalArgumentException("Meal name is required");
        if (order < 0) throw new IllegalArgumentException("Meal order cannot be negative");
        notes = AllowedSubstitution.normalized(notes);
        items = List.copyOf(items == null ? List.of() : items);
        if (items.stream().map(PrescribedMealItem::order).distinct().count() != items.size())
            throw new IllegalArgumentException("Duplicate meal item order");
    }

    public static PrescribedMeal create(String name, int order, LocalTime time, String notes,
                                         List<PrescribedMealItem> items) {
        return new PrescribedMeal(UUID.randomUUID(), name, order, time, notes, null, items);
    }
}

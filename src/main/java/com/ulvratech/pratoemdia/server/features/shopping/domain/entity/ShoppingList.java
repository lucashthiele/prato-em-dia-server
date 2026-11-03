package com.ulvratech.pratoemdia.server.features.shopping.domain.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Snapshot, per weekly plan, of the prescribed foods summed by food, portion and unit. */
public record ShoppingList(UUID id, UUID weeklyPlanId, UUID patientId, Instant createdAt,
        List<ShoppingListItem> items) {
    public ShoppingList {
        items = List.copyOf(items);
    }
}

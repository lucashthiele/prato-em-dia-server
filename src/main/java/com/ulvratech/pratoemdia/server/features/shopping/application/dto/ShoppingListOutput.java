package com.ulvratech.pratoemdia.server.features.shopping.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoppingListOutput(UUID id, UUID weeklyPlanId, Instant createdAt, List<ItemOutput> items) {
    /** The app keeps "bought" by foodId + portionId + unit. */
    public record ItemOutput(UUID foodId, String foodName, UUID portionId, String portionLabel,
            String unit, BigDecimal quantity) {
    }

    /** {@code created} is false when the plan already had an active list. */
    public record Result(boolean created, ShoppingListOutput list) {
    }

    public record Actor(UUID accountId, String role) {
    }
}

package com.ulvratech.pratoemdia.server.features.tracking.application.dto;

import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TrackingData {
    private TrackingData() {
    }

    public record Actor(UUID accountId, String role) {
    }

    public record ConsumedItem(UUID catalogFoodId, UUID catalogPortionId, String unit,
            BigDecimal quantity, BigDecimal multiplier, UUID plannedItemId,
            UUID allowedSubstitutionId) {
    }

    public record Create(Actor actor, UUID plannedMealId, String idempotencyKey,
            String reason, Instant occurredAt, List<ConsumedItem> consumedItems) {
        public Create {
            consumedItems = List.copyOf(consumedItems == null ? List.of() : consumedItems);
        }
    }

    public record CanonicalConversion(BigDecimal grams) {
    }

    public record LoggedItem(UUID plannedItemId, UUID catalogFoodId, UUID catalogPortionId,
            String unit, BigDecimal quantity, BigDecimal multiplier,
            CanonicalConversion canonicalConversion, BigDecimal kcalSnapshot,
            UUID allowedSubstitutionId) {
    }

    public record FoodLogItem(UUID plannedItemId, UUID catalogFoodId, UUID catalogPortionId,
            String unit, BigDecimal quantity, BigDecimal multiplier,
            CanonicalConversion canonicalConversion, BigDecimal kcalSnapshot,
            List<LoggedItem> allowedSubstitutions) {
    }

    public record FoodLog(UUID plannedMealId, PlanningMode mode, List<FoodLogItem> items) {
    }

    public record Event(UUID id, long sequenceId, UUID patientId, UUID weeklyPlanId,
            UUID plannedMealId, Type eventType, String reason, Instant occurredAt,
            Instant createdAt, Object mealSnapshot, Object plannedItemsSnapshot,
            Object consumedItemsSnapshot) {
    }

    public record Summary(UUID weeklyPlanId, int total, int completed, int notCompleted,
            int substituted, int unrecorded) {
    }
}

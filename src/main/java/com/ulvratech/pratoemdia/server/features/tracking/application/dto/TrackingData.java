package com.ulvratech.pratoemdia.server.features.tracking.application.dto;

import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TrackingData {
    private TrackingData() {
    }

    public record Actor(UUID accountId, String role) {
    }

    public record Choice(UUID plannedMealItemId, UUID allowedSubstitutionId) {
    }

    public record Create(Actor actor, UUID plannedMealId, String idempotencyKey, Type eventType,
            String reason, Instant occurredAt, List<Choice> choices) {
        public Create {
            choices = List.copyOf(choices == null ? List.of() : choices);
        }
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

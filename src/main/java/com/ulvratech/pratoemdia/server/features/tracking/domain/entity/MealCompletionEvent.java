package com.ulvratech.pratoemdia.server.features.tracking.domain.entity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MealCompletionEvent(
        long sequenceId, UUID id, UUID patientId, UUID weeklyPlanId, UUID plannedMealId,
        Type eventType, String reason, Instant occurredAt, Instant createdAt,
        String idempotencyKey, String commandHash, String mealSnapshot,
        String plannedItemsSnapshot, String consumedItemsSnapshot) {
    public enum Type {
        COMPLETED, NOT_COMPLETED, SUBSTITUTED
    }

    public MealCompletionEvent {
        Objects.requireNonNull(id);
        Objects.requireNonNull(patientId);
        Objects.requireNonNull(weeklyPlanId);
        Objects.requireNonNull(plannedMealId);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(commandHash);
        Objects.requireNonNull(mealSnapshot);
        Objects.requireNonNull(plannedItemsSnapshot);
        Objects.requireNonNull(consumedItemsSnapshot);
    }

    public static void validateChoices(Type type, List<?> choices) {
        if (type == Type.SUBSTITUTED && (choices == null || choices.isEmpty()))
            throw new IllegalArgumentException("Substituted events require at least one choice");
        if (type != Type.SUBSTITUTED && choices != null && !choices.isEmpty())
            throw new IllegalArgumentException("Choices are only allowed for substituted events");
    }
}

package com.ulvratech.pratoemdia.server.features.tracking.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ExecutionTypes {
    private ExecutionTypes() {
    }

    public record Actor(UUID accountId, String role) {
    }

    public record ItemInput(UUID foodId, UUID portionId, String unit, BigDecimal quantity,
            BigDecimal multiplier, UUID plannedItemId, UUID allowedSubstitutionId) {
    }

    public record Write(LocalDate date, LocalTime time, MealType mealType,
            UUID plannedMealId, List<ItemInput> items) {
    }

    public record Item(UUID id, UUID foodId, UUID portionId, String unit, BigDecimal quantity,
            BigDecimal multiplier, UUID plannedItemId, UUID allowedSubstitutionId,
            BigDecimal grams, BigDecimal kcal, Map<String, DiaryTypes.NutrientTotal> nutrients) {
    }

    public record Execution(UUID id, long version, LocalDate date, LocalTime time,
            MealType mealType, UUID plannedMealId,
            UUID weeklyPlanId, UUID prescriptionVersionId, String mode, List<Item> items,
            Map<String, DiaryTypes.NutrientTotal> totals, Instant createdAt,
            Instant updatedAt, Instant deletedAt, Instant restoreExpiresAt) {
    }

    public record Deleted(UUID id, long version, UUID restoreToken, Instant restoreExpiresAt) {
    }
}

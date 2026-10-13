package com.ulvratech.pratoemdia.server.features.tracking.application.dto;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class FoodLogTypes {
    private FoodLogTypes() {
    }

    public record Actor(UUID accountId, String role) {
    }

    public record CanonicalConversion(BigDecimal grams) {
    }

    public record Item(UUID plannedItemId, UUID catalogFoodId, UUID catalogPortionId,
            String unit, BigDecimal quantity, BigDecimal multiplier,
            CanonicalConversion canonicalConversion, BigDecimal kcalSnapshot,
            UUID allowedSubstitutionId, List<Item> allowedSubstitutions) {
    }

    public record FoodLog(UUID plannedMealId, PlanningMode mode, List<Item> items) {
    }
}

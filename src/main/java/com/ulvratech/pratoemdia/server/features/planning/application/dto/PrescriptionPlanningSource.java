package com.ulvratech.pratoemdia.server.features.planning.application.dto;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PrescriptionPlanningSource(
        UUID versionId, PlanningMode planningMode, List<MealSource> meals) {
    public record MealSource(
            UUID id, String name, MealType mealType, int order, LocalTime time, List<ItemSource> items) {
    }

    public record ItemSource(
            UUID id,
            UUID foodId,
            UUID portionId,
            BigDecimal quantity,
            String unit,
            int order,
            List<SubstitutionSource> substitutions) {
    }

    public record SubstitutionSource(
            UUID id, UUID foodId, UUID portionId, BigDecimal quantity, String unit) {
    }
}

package com.ulvratech.pratoemdia.server.features.planning.application.dto;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record WeeklyPlanOutput(
        UUID id,
        UUID patientId,
        UUID prescriptionVersionId,
        PlanningMode planningMode,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant confirmationDeadline,
        WeeklyPlanStatus status,
        Instant confirmedAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<PlannedMealOutput> meals) {
    public static WeeklyPlanOutput from(WeeklyPlan plan) {
        return new WeeklyPlanOutput(
                plan.getId(),
                plan.getPatientId(),
                plan.getPrescriptionVersionId(),
                plan.getPlanningMode(),
                plan.getPeriodStart(),
                plan.getPeriodEnd(),
                plan.getConfirmationDeadline(),
                plan.getStatus(),
                plan.getConfirmedAt(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getPersistenceVersion(),
                plan.getMeals().stream()
                        .sorted(Comparator.comparing(PlannedMeal::date).thenComparingInt(PlannedMeal::order))
                        .map(PlannedMealOutput::from)
                        .toList()
        );
    }

    public record PlannedMealOutput(
            UUID id,
            UUID prescribedMealId,
            LocalDate date,
            LocalTime time,
            MealType mealType,
            String name,
            int order,
            List<PlannedItemOutput> items) {
        static PlannedMealOutput from(PlannedMeal meal) {
            return new PlannedMealOutput(
                    meal.id(),
                    meal.prescribedMealId(),
                    meal.date(),
                    meal.time(),
                    meal.mealType(),
                    meal.name(),
                    meal.order(),
                    meal.items().stream()
                            .sorted(Comparator.comparingInt(PlannedMealItem::order))
                            .map(PlannedItemOutput::from)
                            .toList()
            );
        }
    }

    public record PlannedItemOutput(
            UUID id,
            UUID prescribedMealItemId,
            UUID allowedSubstitutionId,
            UUID foodId,
            UUID foodPortionId,
            BigDecimal quantity,
            String unit,
            int order) {
        static PlannedItemOutput from(PlannedMealItem item) {
            return new PlannedItemOutput(
                    item.id(),
                    item.prescribedMealItemId(),
                    item.allowedSubstitutionId(),
                    item.foodId(),
                    item.foodPortionId(),
                    item.quantity(),
                    item.unit(),
                    item.order()
            );
        }
    }
}

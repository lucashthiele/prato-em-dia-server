package com.ulvratech.pratoemdia.server.features.planning.application.dto;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PlanGeneratedNotice(
        UUID patientId,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant confirmationDeadline,
        boolean automatic,
        int mealCount,
        List<MealType> dailyMealTypes) {
    public PlanGeneratedNotice {
        dailyMealTypes = List.copyOf(dailyMealTypes);
    }

    public static PlanGeneratedNotice from(WeeklyPlan plan, boolean automatic) {
        var dailyMealTypes = plan.getMeals().stream()
                .filter(meal -> meal.date().equals(plan.getPeriodStart()))
                .sorted(Comparator.comparingInt(PlannedMeal::order))
                .map(PlannedMeal::mealType)
                .toList();
        return new PlanGeneratedNotice(
                plan.getPatientId(),
                plan.getPeriodStart(),
                plan.getPeriodEnd(),
                plan.getConfirmationDeadline(),
                automatic,
                plan.getMeals().size(),
                dailyMealTypes
        );
    }

    public int mealsPerDay() {
        return dailyMealTypes.size();
    }
}

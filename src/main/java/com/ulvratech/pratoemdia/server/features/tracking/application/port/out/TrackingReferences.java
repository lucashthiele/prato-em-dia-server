package com.ulvratech.pratoemdia.server.features.tracking.application.port.out;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import java.util.Optional;
import java.util.UUID;

public interface TrackingReferences {
    Optional<UUID> patientId(UUID accountId);

    boolean hasActiveRelationship(UUID nutritionistAccountId, UUID patientId);

    Optional<WeeklyPlan> plan(UUID planId);

    Optional<WeeklyPlan> planForMeal(UUID mealId);

    Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId);

    Optional<Food> activeFood(UUID foodId);

    Optional<FoodPortion> activePortion(UUID foodId, UUID portionId);
}

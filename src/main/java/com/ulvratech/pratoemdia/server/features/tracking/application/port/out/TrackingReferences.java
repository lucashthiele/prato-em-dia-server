package com.ulvratech.pratoemdia.server.features.tracking.application.port.out;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import java.util.Optional;
import java.util.UUID;

public interface TrackingReferences {
    Optional<UUID> patientId(UUID accountId);

    boolean hasActiveRelationship(UUID nutritionistAccountId, UUID patientId);

    Optional<WeeklyPlan> plan(UUID planId);

    Optional<WeeklyPlan> planForMeal(UUID mealId);

    Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId);
}

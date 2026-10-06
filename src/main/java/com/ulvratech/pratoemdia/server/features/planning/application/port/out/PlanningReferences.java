package com.ulvratech.pratoemdia.server.features.planning.application.port.out;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import java.util.Optional;
import java.util.UUID;

public interface PlanningReferences {
    Optional<UUID> patientProfileId(UUID userAccountId);

    boolean hasActiveRelationship(UUID nutritionistAccountId, UUID patientId);

    Optional<PrescriptionPlanningSource> currentPublishedPrescription(UUID patientId);

    Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId);

    boolean isActiveFood(UUID foodId);

    boolean portionBelongsToFood(UUID portionId, UUID foodId);
}

package com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionReferences {
  Optional<UUID> nutritionistProfileId(UUID accountId);

  Optional<UUID> patientProfileId(UUID accountId);

  boolean hasActiveRelationship(UUID nutritionistId, UUID patientId);

  boolean isActiveFood(UUID foodId);

  boolean portionBelongsToFood(UUID portionId, UUID foodId);
}

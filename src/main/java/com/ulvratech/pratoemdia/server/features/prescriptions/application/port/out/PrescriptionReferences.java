package com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionReferences {
    Optional<UUID> nutritionistProfileId(UUID accountId);

    Optional<UUID> patientProfileId(UUID accountId);

    boolean hasActiveRelationship(UUID nutritionistId, UUID patientId);

    boolean isActiveFood(UUID foodId);

    boolean portionBelongsToFood(UUID portionId, UUID foodId);

    Map<UUID, Food> foods(Collection<UUID> foodIds);

    Map<UUID, FoodPortion> portions(Collection<UUID> portionIds);
}

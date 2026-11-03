package com.ulvratech.pratoemdia.server.features.shopping.application.port.out;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingReferences {
    Optional<UUID> patientId(UUID accountId);

    /** Patient that owns the plan, if the plan exists and was not deleted. */
    Optional<UUID> activePlanOwner(UUID planId);

    Map<UUID, Food> foods(Collection<UUID> foodIds);

    Map<UUID, FoodPortion> portions(Collection<UUID> portionIds);
}

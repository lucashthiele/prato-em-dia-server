package com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.shopping.application.port.out.ShoppingReferences;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CatalogBatchLookup;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShoppingReferencesAdapter implements ShoppingReferences {
    private final ProfileQueryAdapter profiles;
    private final PlanningQueryAdapter planning;
    private final CatalogBatchLookup catalog;

    ShoppingReferencesAdapter(
            ProfileQueryAdapter profiles, PlanningQueryAdapter planning, CatalogBatchLookup catalog
    ) {
        this.profiles = profiles;
        this.planning = planning;
        this.catalog = catalog;
    }

    @Override
    public Optional<UUID> patientId(UUID accountId) {
        return profiles.patientProfileId(accountId);
    }

    @Override
    public Optional<UUID> activePlanOwner(UUID planId) {
        return planning.activePlanOwner(planId);
    }

    @Override
    public Map<UUID, Food> foods(Collection<UUID> foodIds) {
        return catalog.foodsByIds(foodIds);
    }

    @Override
    public Map<UUID, FoodPortion> portions(Collection<UUID> portionIds) {
        return catalog.portionsByIds(portionIds);
    }
}

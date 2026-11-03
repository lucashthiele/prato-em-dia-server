package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter.CatalogQueryAdapter;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter.RelationshipQueryAdapter;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CatalogBatchLookup;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionReferencesAdapter implements PrescriptionReferences {
    private final ProfileQueryAdapter profiles;
    private final RelationshipQueryAdapter relationships;
    private final CatalogQueryAdapter catalog;
    private final CatalogBatchLookup lookup;

    public PrescriptionReferencesAdapter(
            ProfileQueryAdapter profiles, RelationshipQueryAdapter relationships,
            CatalogQueryAdapter catalog, CatalogBatchLookup lookup
    ) {
        this.profiles = profiles;
        this.relationships = relationships;
        this.catalog = catalog;
        this.lookup = lookup;
    }

    @Override
    public Optional<UUID> nutritionistProfileId(UUID accountId) {
        return profiles.nutritionistProfileId(accountId);
    }

    @Override
    public Optional<UUID> patientProfileId(UUID accountId) {
        return profiles.patientProfileId(accountId);
    }

    @Override
    public boolean hasActiveRelationship(UUID nutritionistId, UUID patientId) {
        return relationships.hasActiveRelationship(nutritionistId, patientId);
    }

    @Override
    public boolean isActiveFood(UUID foodId) {
        return catalog.isActiveFood(foodId);
    }

    @Override
    public boolean portionBelongsToFood(UUID portionId, UUID foodId) {
        return catalog.portionBelongsToFood(portionId, foodId);
    }

    @Override
    public Map<UUID, Food> foods(Collection<UUID> foodIds) {
        return lookup.foodsByIds(foodIds);
    }

    @Override
    public Map<UUID, FoodPortion> portions(Collection<UUID> portionIds) {
        return lookup.portionsByIds(portionIds);
    }
}

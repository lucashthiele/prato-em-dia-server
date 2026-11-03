package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CatalogBatchLookup;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter.CatalogQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter.RelationshipQueryAdapter;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.TrackingReferences;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TrackingReferencesAdapter implements TrackingReferences {
    private final PlanningQueryAdapter planning;
    private final PlanningReferences prescriptions;
    private final RelationshipQueryAdapter relationships;
    private final CatalogQueryAdapter catalog;
    private final CatalogBatchLookup lookup;

    public TrackingReferencesAdapter(
            PlanningQueryAdapter planning, PlanningReferences prescriptions,
            RelationshipQueryAdapter relationships, CatalogQueryAdapter catalog,
            CatalogBatchLookup lookup
    ) {
        this.planning = planning;
        this.prescriptions = prescriptions;
        this.relationships = relationships;
        this.catalog = catalog;
        this.lookup = lookup;
    }

    @Override
    public Optional<UUID> patientId(UUID accountId) {
        return prescriptions.patientProfileId(accountId);
    }

    @Override
    public boolean hasActiveRelationship(UUID accountId, UUID patientId) {
        return relationships.hasActiveRelationshipForNutritionistAccount(accountId, patientId);
    }

    @Override
    public Optional<WeeklyPlan> plan(UUID planId) {
        return planning.plan(planId);
    }

    @Override
    public Optional<WeeklyPlan> planForMeal(UUID mealId) {
        return planning.planForMeal(mealId);
    }

    @Override
    public Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId) {
        return prescriptions.prescriptionVersion(versionId);
    }

    @Override
    public Optional<Food> activeFood(UUID foodId) {
        return catalog.findActiveFood(foodId);
    }

    @Override
    public Map<UUID, FoodPortion> activePortions(Collection<UUID> portionIds) {
        var result = new HashMap<UUID, FoodPortion>();
        lookup.portionsByIds(portionIds).forEach((id, portion) -> {
            if (portion.isActive())
                result.put(id, portion);
        });
        return result;
    }
}

package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter.CatalogQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.ItemSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.MealSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.SubstitutionSource;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter.PrescriptionQueryAdapter;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter.PrescriptionQueryAdapter.Version;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter.RelationshipQueryAdapter;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlanningReferencesAdapter implements PlanningReferences {
    private final ProfileQueryAdapter profiles;
    private final RelationshipQueryAdapter relationships;
    private final PrescriptionQueryAdapter prescriptions;
    private final CatalogQueryAdapter catalog;

    public PlanningReferencesAdapter(
            ProfileQueryAdapter profiles, RelationshipQueryAdapter relationships,
            PrescriptionQueryAdapter prescriptions, CatalogQueryAdapter catalog
    ) {
        this.profiles = profiles;
        this.relationships = relationships;
        this.prescriptions = prescriptions;
        this.catalog = catalog;
    }

    @Override
    public Optional<UUID> patientProfileId(UUID userAccountId) {
        return profiles.patientProfileId(userAccountId);
    }

    @Override
    public boolean hasActiveRelationship(UUID accountId, UUID patientId) {
        return relationships.hasActiveRelationshipForNutritionistAccount(accountId, patientId);
    }

    @Override
    public Optional<PrescriptionPlanningSource> currentPublishedPrescription(UUID patientId) {
        return prescriptions.currentPublishedVersion(patientId).map(this::source);
    }

    @Override
    public Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId) {
        return prescriptions.version(versionId).map(this::source);
    }

    @Override
    public boolean isActiveFood(UUID foodId) {
        return catalog.isActiveFood(foodId);
    }

    @Override
    public boolean portionBelongsToFood(UUID portionId, UUID foodId) {
        return catalog.portionBelongsToFood(portionId, foodId);
    }

    private PrescriptionPlanningSource source(Version version) {
        return new PrescriptionPlanningSource(
                version.id(),
                version.planningMode(),
                version.meals().stream()
                        .map(
                                meal -> new MealSource(
                                        meal.id(), meal.name(), meal.mealType(), meal.order(),
                                        meal.suggestedTime(),
                                        meal.items().stream()
                                                .map(
                                                        item -> new ItemSource(
                                                                item.id(), item.foodId(),
                                                                item.portionId(), item.quantity(),
                                                                item.unit(), item.order(),
                                                                item.substitutions().stream()
                                                                        .map(
                                                                                sub -> new SubstitutionSource(
                                                                                        sub.id(), sub.foodId(),
                                                                                        sub.portionId(),
                                                                                        sub.quantity(),
                                                                                        sub.unit()
                                                                                )
                                                                        ).toList()
                                                        )
                                                ).toList()
                                )
                        ).toList()
        );
    }
}

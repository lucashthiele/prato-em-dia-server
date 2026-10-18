package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter.CatalogQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter.PlanSlot;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter.PlannedItemRef;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter.PrescriptionQueryAdapter;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Lookups into planning, prescriptions and catalog used to validate executions. */
@Component
class ExecutionLookups {
    private final PlanningQueryAdapter planning;
    private final PrescriptionQueryAdapter prescriptions;
    private final CatalogQueryAdapter catalog;

    ExecutionLookups(
            PlanningQueryAdapter planning, PrescriptionQueryAdapter prescriptions,
            CatalogQueryAdapter catalog
    ) {
        this.planning = planning;
        this.prescriptions = prescriptions;
        this.catalog = catalog;
    }

    Optional<UUID> publishedFreeVersion(UUID patientId) {
        return prescriptions.publishedFreeVersionId(patientId);
    }

    Optional<PlanSlot> planSlot(UUID plannedMealId, UUID patientId) {
        return planning.planSlot(plannedMealId, patientId);
    }

    Optional<PlannedItemRef> plannedItem(UUID plannedItemId, UUID plannedMealId) {
        return planning.plannedItem(plannedItemId, plannedMealId);
    }

    long plannedItemCount(UUID plannedMealId) {
        return planning.plannedItemCount(plannedMealId);
    }

    boolean substitutionAuthorized(
            UUID substitutionId, UUID prescribedItemId, UUID foodId, UUID portionId
    ) {
        return prescriptions.substitutionAuthorized(substitutionId, prescribedItemId, foodId, portionId);
    }

    Optional<CatalogQueryAdapter.Portion> activePortion(UUID foodId, UUID portionId) {
        return catalog.activePortion(foodId, portionId);
    }
}

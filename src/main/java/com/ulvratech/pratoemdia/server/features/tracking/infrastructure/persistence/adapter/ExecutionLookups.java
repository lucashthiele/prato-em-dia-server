package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealItemModel;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Read-only lookups into the planning, prescriptions and catalog models used to validate executions. */
@Component
class ExecutionLookups {
    record PlanSlot(UUID planId, UUID versionId, PlanningMode mode, LocalDate mealDate,
            LocalTime mealTime, MealType mealType) {
    }

    record PlannedItemRef(UUID foodId, UUID prescribedItemId) {
    }

    private final EntityManager entityManager;

    ExecutionLookups(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    Optional<UUID> publishedFreeVersion(UUID patientId) {
        return entityManager.createQuery("""
                select v.id from PrescriptionVersionModel v
                where v.prescription.patientId = :patient
                  and v.prescription.status = :prescriptionStatus
                  and v.prescription.planningMode = :mode
                  and v.status = :versionStatus
                order by v.publishedAt desc
                """, UUID.class)
                .setParameter("patient", patientId)
                .setParameter("prescriptionStatus", PrescriptionStatus.ACTIVE)
                .setParameter("mode", PlanningMode.FREE)
                .setParameter("versionStatus", PrescriptionVersionStatus.PUBLISHED)
                .setMaxResults(1).getResultStream().findFirst();
    }

    Optional<PlanSlot> planSlot(UUID plannedMealId, UUID patientId) {
        return entityManager.createQuery("""
                select m from PlannedMealModel m
                where m.id = :id and m.plan.patientId = :patient
                """, PlannedMealModel.class)
                .setParameter("id", plannedMealId).setParameter("patient", patientId)
                .getResultStream().findFirst()
                .map(
                        meal -> new PlanSlot(
                                meal.getPlan().getId(), meal.getPlan().getPrescriptionVersionId(),
                                meal.getPlan().getPlanningMode(), meal.getDate(), meal.getTime(),
                                meal.getMealType()
                        )
                );
    }

    Optional<PlannedItemRef> plannedItem(UUID plannedItemId, UUID plannedMealId) {
        return entityManager.createQuery("""
                select i from PlannedMealItemModel i
                where i.id = :id and i.meal.id = :meal
                """, PlannedMealItemModel.class)
                .setParameter("id", plannedItemId).setParameter("meal", plannedMealId)
                .getResultStream().findFirst()
                .map(item -> new PlannedItemRef(item.getFoodId(), item.getPrescribedMealItemId()));
    }

    long plannedItemCount(UUID plannedMealId) {
        return entityManager.createQuery(
                "select count(i) from PlannedMealItemModel i where i.meal.id = :meal", Long.class
        ).setParameter("meal", plannedMealId).getSingleResult();
    }

    boolean substitutionAuthorized(
            UUID substitutionId, UUID prescribedItemId, UUID foodId, UUID portionId
    ) {
        return entityManager.createQuery("""
                select count(s) from AllowedSubstitutionModel s
                where s.id = :id and s.item.id = :item and s.foodId = :food
                  and (s.foodPortionId is null or s.foodPortionId = :portion)
                """, Long.class)
                .setParameter("id", substitutionId).setParameter("item", prescribedItemId)
                .setParameter("food", foodId).setParameter("portion", portionId)
                .getSingleResult() > 0;
    }

    Optional<FoodPortionModel> activePortion(UUID foodId, UUID portionId) {
        return entityManager.createQuery("""
                select p from FoodPortionModel p, FoodModel f
                where f.id = p.foodId and f.id = :food and p.id = :portion
                  and f.active = true and p.active = true
                """, FoodPortionModel.class)
                .setParameter("food", foodId).setParameter("portion", portionId)
                .getResultStream().findFirst();
    }
}

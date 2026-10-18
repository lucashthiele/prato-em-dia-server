package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionReferencesAdapter implements PrescriptionReferences {
    private final EntityManager entityManager;

    public PrescriptionReferencesAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<UUID> nutritionistProfileId(UUID accountId) {
        return entityManager.createQuery(
                "select p.id from NutritionistProfileModel p where p.userAccountId = :account",
                UUID.class
        ).setParameter("account", accountId).getResultStream().findFirst();
    }

    @Override
    public Optional<UUID> patientProfileId(UUID accountId) {
        return entityManager.createQuery(
                "select p.id from PatientProfileModel p where p.userAccountId = :account",
                UUID.class
        ).setParameter("account", accountId).getResultStream().findFirst();
    }

    @Override
    public boolean hasActiveRelationship(UUID nutritionistId, UUID patientId) {
        return entityManager.createQuery("""
                select count(r) from RelationshipModel r
                where r.nutritionistId = :nutritionist and r.patientId = :patient
                  and r.status = :status
                """, Long.class)
                .setParameter("nutritionist", nutritionistId).setParameter("patient", patientId)
                .setParameter("status", RelationshipStatus.ACTIVE).getSingleResult() > 0;
    }

    @Override
    public boolean isActiveFood(UUID foodId) {
        return entityManager.createQuery(
                "select count(f) from FoodModel f where f.id = :id and f.active = true", Long.class
        ).setParameter("id", foodId).getSingleResult() > 0;
    }

    @Override
    public boolean portionBelongsToFood(UUID portionId, UUID foodId) {
        return entityManager.createQuery("""
                select count(p) from FoodPortionModel p
                where p.id = :portion and p.foodId = :food and p.active = true
                """, Long.class)
                .setParameter("portion", portionId).setParameter("food", foodId)
                .getSingleResult() > 0;
    }
}

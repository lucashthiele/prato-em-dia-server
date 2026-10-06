package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.mapper.WeeklyPlanningMapper;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanModel;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.TrackingReferences;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TrackingReferencesAdapter implements TrackingReferences {
    private final EntityManager entityManager;
    private final WeeklyPlanningMapper mapper;
    private final PlanningReferences prescriptions;

    public TrackingReferencesAdapter(
            EntityManager entityManager, WeeklyPlanningMapper mapper,
            PlanningReferences prescriptions
    ) {
        this.entityManager = entityManager;
        this.mapper = mapper;
        this.prescriptions = prescriptions;
    }

    @Override
    public Optional<UUID> patientId(UUID accountId) {
        return prescriptions.patientProfileId(accountId);
    }

    @Override
    public boolean hasActiveRelationship(UUID accountId, UUID patientId) {
        return !entityManager.createNativeQuery(

                """
                        SELECT r.id FROM nutritionist_patient_relationship r
                        JOIN nutritionist_profile n ON n.id = r.nutritionist_id
                        WHERE n.user_account_id = :account AND r.patient_id = :patient
                          AND r.status = 'ACTIVE'
                        """
        ).setParameter("account", accountId).setParameter("patient", patientId)
                .setMaxResults(1).getResultList().isEmpty();
    }

    @Override
    public Optional<WeeklyPlan> plan(UUID planId) {
        return Optional.ofNullable(entityManager.find(WeeklyPlanModel.class, planId)).map(mapper::toDomain);
    }

    @Override
    public Optional<WeeklyPlan> planForMeal(UUID mealId) {
        return entityManager.createQuery(
                "select p from WeeklyPlanModel p join p.meals m where m.id=:id",
                WeeklyPlanModel.class
        ).setParameter("id", mealId).getResultStream()
                .findFirst().map(mapper::toDomain);
    }

    @Override
    public Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId) {
        return prescriptions.prescriptionVersion(versionId);
    }
}

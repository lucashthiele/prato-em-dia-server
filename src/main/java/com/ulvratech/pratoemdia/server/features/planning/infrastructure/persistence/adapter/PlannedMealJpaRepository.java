package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlannedMealJpaRepository extends JpaRepository<PlannedMealModel, UUID> {
    Optional<PlannedMealModel> findByIdAndPlanPatientIdAndPlanActiveTrue(UUID id, UUID patientId);
}

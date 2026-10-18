package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.NutritionTargetModel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NutritionTargetJpaRepository extends JpaRepository<NutritionTargetModel, UUID> {
    List<NutritionTargetModel> findAllByVersionId(UUID versionId);
}

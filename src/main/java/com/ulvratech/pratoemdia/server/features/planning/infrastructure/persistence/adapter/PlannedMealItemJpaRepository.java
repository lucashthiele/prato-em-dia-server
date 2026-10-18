package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealItemModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlannedMealItemJpaRepository extends JpaRepository<PlannedMealItemModel, UUID> {
    Optional<PlannedMealItemModel> findByIdAndMealId(UUID id, UUID mealId);

    long countByMealId(UUID mealId);
}

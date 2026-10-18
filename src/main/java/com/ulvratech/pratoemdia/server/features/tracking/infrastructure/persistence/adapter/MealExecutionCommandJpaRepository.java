package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionCommandModel;
import org.springframework.data.jpa.repository.JpaRepository;

interface MealExecutionCommandJpaRepository
        extends JpaRepository<MealExecutionCommandModel, MealExecutionCommandModel.Key> {
}

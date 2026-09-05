package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FoodPortionJpaRepository extends JpaRepository<FoodPortionModel, UUID> {
    List<FoodPortionModel> findAllByFoodIdAndActiveTrueOrderByLabelAsc(UUID foodId);
}

package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodPortionJpaRepository extends JpaRepository<FoodPortionModel, UUID> {
    @EntityGraph(attributePaths = "nutrients")
    List<FoodPortionModel> findAllByFoodIdAndActiveTrueOrderByLabelAsc(UUID foodId);

    @EntityGraph(attributePaths = "nutrients")
    List<FoodPortionModel> findAllByFoodIdInAndSourceTypeIsNotNull(List<UUID> foodIds);

    @EntityGraph(attributePaths = "nutrients")
    Optional<FoodPortionModel> findByIdAndFoodIdAndActiveTrue(UUID id, UUID foodId);

    boolean existsByIdAndFoodIdAndActiveTrue(UUID id, UUID foodId);

    void deleteAllByFoodIdInAndSourceTypeIsNotNull(List<UUID> foodIds);
}

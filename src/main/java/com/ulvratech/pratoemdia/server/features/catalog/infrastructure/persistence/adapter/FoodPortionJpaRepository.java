package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodPortionJpaRepository extends JpaRepository<FoodPortionModel, UUID> {
  @EntityGraph(attributePaths = "nutrients")
  List<FoodPortionModel> findAllByFoodIdAndActiveTrueOrderByLabelAsc(UUID foodId);

  @EntityGraph(attributePaths = "nutrients")
  List<FoodPortionModel> findAllByFoodIdInAndSourceTypeIsNotNull(List<UUID> foodIds);

  void deleteAllByFoodIdInAndSourceTypeIsNotNull(List<UUID> foodIds);
}

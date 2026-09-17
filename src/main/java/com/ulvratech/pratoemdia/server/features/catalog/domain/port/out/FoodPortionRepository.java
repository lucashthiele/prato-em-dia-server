package com.ulvratech.pratoemdia.server.features.catalog.domain.port.out;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FoodPortionRepository {
  List<FoodPortion> saveAll(List<FoodPortion> portions);

  List<FoodPortion> findActiveByFoodId(UUID foodId);

  List<FoodPortion> findImportedByFoodIds(Collection<UUID> foodIds);

  List<FoodPortion> replaceImportedForFoods(
      Collection<UUID> foodIds, Collection<FoodPortion> portions);
}

package com.ulvratech.pratoemdia.server.features.catalog.domain.port.out;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface FoodRepository {
    Food save(Food food);
    Optional<Food> findById(UUID id);
    Optional<Food> findActiveById(UUID id);
    PageResult<Food> findActive(PageRequest page);
}

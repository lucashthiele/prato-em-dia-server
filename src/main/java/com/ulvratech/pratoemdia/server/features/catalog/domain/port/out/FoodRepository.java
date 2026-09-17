package com.ulvratech.pratoemdia.server.features.catalog.domain.port.out;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FoodRepository {
    Food save(Food food);

    Optional<Food> findById(UUID id);

    Optional<Food> findActiveById(UUID id);

    PageResult<Food> findActive(PageRequest page);

    List<Food> findBySourceAndExternalIds(String source, Collection<String> externalIds);

    List<Food> saveAll(Collection<Food> foods);
}

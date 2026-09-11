package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper.FoodPortionMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Collection;

@Repository
@Transactional
public class FoodPortionRepositoryAdapter implements FoodPortionRepository {
    private final FoodPortionJpaRepository repository;
    private final FoodPortionMapper mapper;

    public FoodPortionRepositoryAdapter(
        FoodPortionJpaRepository repository, FoodPortionMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<FoodPortion> saveAll(List<FoodPortion> portions) {
        var models = portions.stream().map(mapper::toModel).toList();
        return repository.saveAll(models).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<FoodPortion> findActiveByFoodId(UUID foodId) {
        return repository.findAllByFoodIdAndActiveTrueOrderByLabelAsc(foodId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<FoodPortion> findImportedByFoodIds(Collection<UUID> foodIds) {
        if (foodIds.isEmpty()) return List.of();
        return repository.findAllByFoodIdInAndSourceTypeIsNotNull(List.copyOf(foodIds)).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<FoodPortion> replaceImportedForFoods(
        Collection<UUID> foodIds, Collection<FoodPortion> portions
    ) {
        if (foodIds.isEmpty()) return List.of();
        repository.deleteAllByFoodIdInAndSourceTypeIsNotNull(List.copyOf(foodIds));
        repository.flush();
        return repository.saveAll(portions.stream().map(mapper::toModel).toList()).stream()
            .map(mapper::toDomain)
            .toList();
    }
}

package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper.FoodPortionMapper;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import java.util.stream.Collectors;
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
        if (foodIds.isEmpty())
            return List.of();
        return repository.findAllByFoodIdInAndSourceTypeIsNotNull(List.copyOf(foodIds)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FoodPortion> replaceImportedForFoods(
            Collection<UUID> foodIds, Collection<FoodPortion> portions
    ) {
        if (foodIds.isEmpty())
            return List.of();
        // Imported portion ids are deterministic, so a portion that still exists is updated in place: deleting and
        // re-inserting it broke the prescriptions and plans that reference it (food_portion foreign keys). Only
        // portions the source no longer publishes are removed.
        var desired = portions.stream().map(FoodPortion::getId).collect(Collectors.toSet());
        var stale = repository.findAllByFoodIdInAndSourceTypeIsNotNull(List.copyOf(foodIds)).stream()
                .map(FoodPortionModel::getId)
                .filter(id -> !desired.contains(id))
                .toList();
        if (!stale.isEmpty()) {
            repository.deleteAllByIdInBatch(stale);
            repository.flush();
        }
        return repository.saveAll(portions.stream().map(mapper::toModel).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

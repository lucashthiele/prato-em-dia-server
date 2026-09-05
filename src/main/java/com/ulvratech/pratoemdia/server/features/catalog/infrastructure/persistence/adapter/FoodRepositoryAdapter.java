package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper.FoodMapper;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class FoodRepositoryAdapter implements FoodRepository {
    private final FoodJpaRepository repository;
    private final FoodMapper mapper;

    public FoodRepositoryAdapter(FoodJpaRepository repository, FoodMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Food save(Food food) {
        return mapper.toDomain(repository.save(mapper.toModel(food)));
    }

    @Override
    public Optional<Food> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Food> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Food> findActive(PageRequest request) {
        var pageable = org.springframework.data.domain.PageRequest.of(
            request.page(), request.size(), Sort.by(Sort.Direction.ASC, "name")
        );
        var page = repository.findAllByActiveTrue(pageable);
        return new PageResult<>(
            page.getContent().stream().map(mapper::toDomain).toList(),
            page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }
}

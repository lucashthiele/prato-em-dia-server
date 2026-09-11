package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FoodJpaRepository extends JpaRepository<FoodModel, UUID> {
    @Override
    Optional<FoodModel> findById(UUID id);

    Optional<FoodModel> findByIdAndActiveTrue(UUID id);
    Page<FoodModel> findAllByActiveTrue(Pageable pageable);

    List<FoodModel> findAllBySourceAndSourceExternalIdIn(
        String source, Collection<String> externalIds
    );
}

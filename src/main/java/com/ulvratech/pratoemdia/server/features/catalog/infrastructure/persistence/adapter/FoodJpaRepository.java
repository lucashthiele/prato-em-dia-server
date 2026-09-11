package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FoodJpaRepository extends JpaRepository<FoodModel, UUID> {
    @Override
    @EntityGraph(attributePaths = "nutrients")
    Optional<FoodModel> findById(UUID id);

    @EntityGraph(attributePaths = "nutrients")
    Optional<FoodModel> findByIdAndActiveTrue(UUID id);
    Page<FoodModel> findAllByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "nutrients")
    List<FoodModel> findAllBySourceAndSourceExternalIdIn(
        String source, Collection<String> externalIds
    );
}

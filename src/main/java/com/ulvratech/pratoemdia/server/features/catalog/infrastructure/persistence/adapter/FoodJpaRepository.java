package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FoodJpaRepository extends JpaRepository<FoodModel, UUID> {
    @Override
    Optional<FoodModel> findById(UUID id);

    Optional<FoodModel> findByIdAndActiveTrue(UUID id);

    boolean existsByIdAndActiveTrue(UUID id);

    Page<FoodModel> findAllByActiveTrue(Pageable pageable);

    @Query(value = "select * from food where active = true and lower(unaccent(name)) like concat('%', :term, '%')", countQuery = "select count(*) from food where active = true and lower(unaccent(name)) like concat('%', :term, '%')", nativeQuery = true)
    Page<FoodModel> searchActive(@Param("term") String normalizedTerm, Pageable pageable);

    List<FoodModel> findAllBySourceAndSourceExternalIdIn(
            String source, Collection<String> externalIds
    );
}

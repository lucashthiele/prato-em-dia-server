package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.NutritionistProfileModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface NutritionistProfileJpaRepository extends JpaRepository<NutritionistProfileModel, UUID> {
    Optional<NutritionistProfileModel> findByUserAccountId(UUID userAccountId);
}

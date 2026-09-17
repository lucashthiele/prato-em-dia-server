package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.NutritionistProfileModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NutritionistProfileJpaRepository extends JpaRepository<NutritionistProfileModel, UUID> {
  Optional<NutritionistProfileModel> findByUserAccountId(UUID userAccountId);
}

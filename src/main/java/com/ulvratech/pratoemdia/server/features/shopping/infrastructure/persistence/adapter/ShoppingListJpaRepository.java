package com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.model.ShoppingListModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShoppingListJpaRepository extends JpaRepository<ShoppingListModel, UUID> {
    Optional<ShoppingListModel> findByWeeklyPlanIdAndActiveTrue(UUID weeklyPlanId);
}

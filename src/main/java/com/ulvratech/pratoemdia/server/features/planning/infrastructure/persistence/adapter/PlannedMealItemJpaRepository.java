package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealItemModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlannedMealItemJpaRepository extends JpaRepository<PlannedMealItemModel, UUID> {
    Optional<PlannedMealItemModel> findByIdAndMealId(UUID id, UUID mealId);

    long countByMealId(UUID mealId);

    /** Items of the plan in plan order: meal date, meal order, item order. */
    @org.springframework.data.jpa.repository.Query("""
            select i from PlannedMealItemModel i join i.meal m
            where m.plan.id = :planId
            order by m.date, m.order, i.order
            """)
    java.util.List<PlannedMealItemModel> findAllOfPlan(
            @org.springframework.data.repository.query.Param("planId") UUID planId
    );
}

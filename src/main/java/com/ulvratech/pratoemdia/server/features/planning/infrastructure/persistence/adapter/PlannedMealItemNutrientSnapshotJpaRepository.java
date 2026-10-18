package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealItemNutrientSnapshotModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PlannedMealItemNutrientSnapshotJpaRepository
        extends JpaRepository<PlannedMealItemNutrientSnapshotModel, PlannedMealItemNutrientSnapshotModel.Key> {
    interface NutrientRow {
        String getCode();

        BigDecimal getAmount();

        String getUnit();
    }

    @Query("""
            select s.nutrientCode as code, sum(s.amount) as amount, s.unit as unit
            from PlannedMealItemNutrientSnapshotModel s, PlannedMealItemModel i
            where i.id = s.plannedItemId and i.meal.plan.id = :planId and i.meal.date = :date
            group by s.nutrientCode, s.unit
            """)
    List<NutrientRow> sumByPlanAndDate(@Param("planId") UUID planId, @Param("date") LocalDate date);
}

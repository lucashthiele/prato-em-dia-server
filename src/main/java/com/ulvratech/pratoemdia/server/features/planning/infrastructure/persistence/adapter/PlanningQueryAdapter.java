package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.mapper.WeeklyPlanningMapper;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of planning data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class PlanningQueryAdapter {
    public record PlanSlot(UUID planId, UUID versionId, PlanningMode mode, LocalDate mealDate,
            LocalTime mealTime, MealType mealType) {
    }

    public record PlannedItemRef(UUID foodId, UUID prescribedItemId) {
    }

    public record PlanPeriod(UUID planId, UUID versionId, LocalDate start, LocalDate end) {
    }

    public record NutrientSum(String code, BigDecimal amount, String unit) {
    }

    private final WeeklyPlanningMapper mapper;
    private final WeeklyPlanJpaRepository plans;
    private final PlannedMealJpaRepository meals;
    private final PlannedMealItemJpaRepository items;
    private final PlannedMealItemNutrientSnapshotJpaRepository snapshots;

    PlanningQueryAdapter(
            WeeklyPlanningMapper mapper, WeeklyPlanJpaRepository plans, PlannedMealJpaRepository meals,
            PlannedMealItemJpaRepository items,
            PlannedMealItemNutrientSnapshotJpaRepository snapshots
    ) {
        this.mapper = mapper;
        this.plans = plans;
        this.meals = meals;
        this.items = items;
        this.snapshots = snapshots;
    }

    public Optional<WeeklyPlan> plan(UUID planId) {
        return plans.findById(planId).map(mapper::toDomain);
    }

    public Optional<WeeklyPlan> planForMeal(UUID plannedMealId) {
        return plans.findDistinctByMealsId(plannedMealId).map(mapper::toDomain);
    }

    public Optional<PlanSlot> planSlot(UUID plannedMealId, UUID patientId) {
        return meals.findByIdAndPlanPatientIdAndPlanActiveTrue(plannedMealId, patientId)
                .map(
                        meal -> new PlanSlot(
                                meal.getPlan().getId(), meal.getPlan().getPrescriptionVersionId(),
                                meal.getPlan().getPlanningMode(), meal.getDate(), meal.getTime(),
                                meal.getMealType()
                        )
                );
    }

    public Optional<PlannedItemRef> plannedItem(UUID plannedItemId, UUID plannedMealId) {
        return items.findByIdAndMealId(plannedItemId, plannedMealId)
                .map(item -> new PlannedItemRef(item.getFoodId(), item.getPrescribedMealItemId()));
    }

    public long plannedItemCount(UUID plannedMealId) {
        return items.countByMealId(plannedMealId);
    }

    public List<PlanPeriod> planPeriods(UUID patientId) {
        return plans.findAllByPatientIdOrderByPeriodStartAsc(patientId).stream()
                .map(this::period).toList();
    }

    public Optional<PlanPeriod> planCovering(UUID patientId, LocalDate date) {
        return plans
                .findFirstByPatientIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
                        patientId, date, date
                ).map(this::period);
    }

    public List<NutrientSum> plannedNutrients(UUID planId, LocalDate date) {
        return snapshots.sumByPlanAndDate(planId, date).stream()
                .map(row -> new NutrientSum(row.getCode(), row.getAmount(), row.getUnit())).toList();
    }

    private PlanPeriod period(
            WeeklyPlanModel plan
    ) {
        return new PlanPeriod(
                plan.getId(), plan.getPrescriptionVersionId(), plan.getPeriodStart(),
                plan.getPeriodEnd()
        );
    }
}

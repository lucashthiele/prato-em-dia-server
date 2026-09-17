package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WeeklyPlanTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 18);
    private static final Instant DURING = Instant.parse("2026-09-18T12:00:00Z");

    @Test
    void spansSevenDaysAndConfirmsIdempotently() {
        var plan = plan(PlanningMode.FREE);
        assertEquals(START.plusDays(6), plan.getPeriodEnd());
        plan.confirm(DURING);
        var confirmedAt = plan.getConfirmedAt();
        plan.confirm(DURING.plusSeconds(10));
        assertEquals(WeeklyPlanStatus.CONFIRMED, plan.getStatus());
        assertEquals(confirmedAt, plan.getConfirmedAt());
    }

    @Test
    void freeAllowsScheduleAndItemChangesOnlyBeforeConfirmation() {
        var plan = plan(PlanningMode.FREE);
        var changed = meal(plan, START.plusDays(1), UUID.randomUUID());
        assertDoesNotThrow(() -> plan.replaceMeals(List.of(changed), DURING));
        plan.confirm(DURING);
        var rescheduled = meal(plan, START.plusDays(2), UUID.randomUUID());
        assertThrows(
                IllegalStateException.class, () -> plan.replaceMeals(List.of(rescheduled), DURING)
        );
        var sameSchedule = new PlannedMeal(
                changed.id(),
                changed.prescribedMealId(),
                changed.date(),
                changed.time(),
                changed.mealType(),
                changed.name(),
                changed.order(),
                List.of()
        );
        assertDoesNotThrow(() -> plan.replaceMeals(List.of(sameSchedule), DURING));
    }

    @Test
    void substitutionsOnlyPreservesScheduleAndItemIdentityBeforeAndAfterConfirmation() {
        var plan = plan(PlanningMode.SUBSTITUTIONS_ONLY);
        var original = plan.getMeals().getFirst();
        var oldItem = original.items().getFirst();
        var substitution = new PlannedMealItem(
                oldItem.id(),
                oldItem.prescribedMealItemId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                BigDecimal.TWO,
                "g",
                oldItem.order()
        );
        var desired = new PlannedMeal(
                original.id(),
                original.prescribedMealId(),
                original.date(),
                original.time(),
                original.mealType(),
                original.name(),
                original.order(),
                List.of(substitution)
        );
        assertDoesNotThrow(() -> plan.replaceMeals(List.of(desired), DURING));
        plan.confirm(DURING);
        assertDoesNotThrow(() -> plan.replaceMeals(List.of(original), DURING));
        assertThrows(IllegalStateException.class, () -> plan.replaceMeals(List.of(), DURING));
    }

    @Test
    void restrictedAndExpiredPlansCannotChange() {
        var restricted = plan(PlanningMode.RESTRICTED);
        assertThrows(
                IllegalStateException.class, () -> restricted.replaceMeals(restricted.getMeals(), DURING)
        );
        var free = plan(PlanningMode.FREE);
        Instant ended = START.plusDays(7).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant();
        assertThrows(IllegalStateException.class, () -> free.replaceMeals(free.getMeals(), ended));
    }

    private WeeklyPlan plan(PlanningMode mode) {
        var meal = new PlannedMeal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                START,
                LocalTime.NOON,
                MealType.LUNCH,
                "Lunch",
                0,
                List.of(
                        new PlannedMealItem(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                null,
                                UUID.randomUUID(),
                                null,
                                BigDecimal.ONE,
                                "g",
                                0
                        )
                )
        );
        return WeeklyPlan.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                mode,
                START,
                START.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(),
                List.of(meal)
        );
    }

    private PlannedMeal meal(WeeklyPlan plan, LocalDate date, UUID food) {
        var old = plan.getMeals().getFirst();
        return new PlannedMeal(
                old.id(),
                old.prescribedMealId(),
                date,
                old.time(),
                old.mealType(),
                old.name(),
                old.order(),
                List.of(
                        new PlannedMealItem(
                                old.items().getFirst().id(),
                                old.items().getFirst().prescribedMealItemId(),
                                null,
                                food,
                                null,
                                BigDecimal.ONE,
                                "g",
                                0
                        )
                )
        );
    }
}

package com.ulvratech.pratoemdia.server.features.tracking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.tracking.domain.MealCategories.DayFacts;
import com.ulvratech.pratoemdia.server.features.tracking.domain.MealCategories.ExecutionFact;
import com.ulvratech.pratoemdia.server.features.tracking.domain.MealCategories.PlannedCategory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MealCategoriesTest {
    final UUID mealId = UUID.randomUUID();
    final UUID rice = UUID.randomUUID();
    final UUID beans = UUID.randomUUID();

    PlannedCategory lunch(PlanningMode mode, UUID... items) {
        return new PlannedCategory(mealId, MealType.LUNCH, mode, List.of(items));
    }

    MealCategories.Status lunchOf(DayFacts facts) {
        return MealCategories.derive(facts).stream()
                .filter(c -> c.mealType() == MealType.LUNCH).findFirst().orElseThrow();
    }

    ExecutionFact execution(UUID... covered) {
        return new ExecutionFact(MealType.LUNCH, mealId, Set.of(covered));
    }

    @Test
    void alwaysReturnsTheFourCategoriesInOrder() {
        var result = MealCategories.derive(new DayFacts(null, List.of(), List.of()));

        assertEquals(
                List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER),
                result.stream().map(MealCategories.Status::mealType).toList()
        );
    }

    @Test
    void restrictedCategoryIsNotLoggedWithoutExecutions() {
        var status = lunchOf(
                new DayFacts(PlanningMode.RESTRICTED, List.of(lunch(PlanningMode.RESTRICTED, rice, beans)), List.of())
        );

        assertEquals(MealCategories.State.NOT_LOGGED, status.state());
        assertTrue(status.canRegister());
        assertNull(status.blockedReason());
        assertEquals(mealId, status.plannedMealId());
        assertEquals(List.of(), status.coveredItemIds());
        assertEquals(List.of(rice, beans), status.uncoveredItemIds());
    }

    @Test
    void restrictedCategoryIsPartialUntilEveryItemIsCoveredByTheUnionOfExecutions() {
        var planned = List.of(lunch(PlanningMode.RESTRICTED, rice, beans));

        var partial = lunchOf(
                new DayFacts(PlanningMode.RESTRICTED, planned, List.of(execution(rice)))
        );
        var logged = lunchOf(
                new DayFacts(PlanningMode.RESTRICTED, planned, List.of(execution(rice), execution(beans)))
        );

        assertEquals(MealCategories.State.PARTIAL, partial.state());
        assertEquals(List.of(rice), partial.coveredItemIds());
        assertEquals(List.of(beans), partial.uncoveredItemIds());
        assertEquals(MealCategories.State.LOGGED, logged.state());
        assertEquals(List.of(), logged.uncoveredItemIds());
    }

    @Test
    void substitutionsOnlyFollowsTheSameCoverageRule() {
        var status = lunchOf(
                new DayFacts(
                        PlanningMode.SUBSTITUTIONS_ONLY,
                        List.of(lunch(PlanningMode.SUBSTITUTIONS_ONLY, rice, beans)),
                        List.of(execution(rice))
                )
        );

        assertEquals(MealCategories.State.PARTIAL, status.state());
    }

    @Test
    void freeCategoryHasNoPartialState() {
        var status = lunchOf(
                new DayFacts(
                        PlanningMode.FREE, List.of(lunch(PlanningMode.FREE, rice, beans)),
                        List.of(execution(rice))
                )
        );

        assertEquals(MealCategories.State.LOGGED, status.state());
        assertTrue(status.canRegister());
    }

    @Test
    void anyActiveExecutionLogsADayWithoutPlan() {
        var free = new ExecutionFact(MealType.LUNCH, null, Set.of());

        var status = lunchOf(new DayFacts(PlanningMode.FREE, List.of(), List.of(free)));

        assertEquals(MealCategories.State.LOGGED, status.state());
        assertNull(status.plannedMealId());
    }

    @Test
    void categoryWithoutItemsBlocksRegistrationInRestrictedAndSubstitutionsOnly() {
        for (var mode : List.of(PlanningMode.RESTRICTED, PlanningMode.SUBSTITUTIONS_ONLY)) {
            var withEmptyMeal = lunchOf(new DayFacts(mode, List.of(lunch(mode)), List.of()));
            var withoutMeal = lunchOf(new DayFacts(mode, List.of(), List.of()));

            for (var status : List.of(withEmptyMeal, withoutMeal)) {
                assertFalse(status.canRegister());
                assertEquals("NO_ALLOWED_FOODS", status.blockedReason());
                assertEquals(MealCategories.State.NOT_LOGGED, status.state());
            }
        }
    }

    @Test
    void categoryWithoutItemsAcceptsFreeRegistrationInFreeMode() {
        var emptyMeal = lunchOf(new DayFacts(PlanningMode.FREE, List.of(lunch(PlanningMode.FREE)), List.of()));
        var noMeal = lunchOf(new DayFacts(PlanningMode.FREE, List.of(), List.of()));

        assertTrue(emptyMeal.canRegister());
        assertTrue(noMeal.canRegister());
    }

    @Test
    void emptyPlannedCategoryWithAnExecutionIsLogged() {
        var status = lunchOf(
                new DayFacts(
                        PlanningMode.RESTRICTED, List.of(lunch(PlanningMode.RESTRICTED)),
                        List.of(new ExecutionFact(MealType.LUNCH, mealId, Set.of()))
                )
        );

        assertEquals(MealCategories.State.LOGGED, status.state());
    }

    @Test
    void withoutAnyPublishedPrescriptionNothingCanBeRegistered() {
        var status = lunchOf(new DayFacts(null, List.of(), List.of()));

        assertFalse(status.canRegister());
        assertEquals("NO_ALLOWED_FOODS", status.blockedReason());
    }
}

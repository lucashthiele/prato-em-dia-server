package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.ItemSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.MealSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.SubstitutionSource;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.FoodLog;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.TrackingReferences;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pins the numbers of the food log so the nutrient scaling can move to the catalog safely. */
class FoodLogUseCaseTest {
    final TrackingReferences references = mock(TrackingReferences.class);
    final FoodLogUseCase useCase = new FoodLogUseCase(references);
    final UUID account = UUID.randomUUID();
    final UUID patient = UUID.randomUUID();
    final UUID versionId = UUID.randomUUID();
    final UUID mealId = UUID.randomUUID();
    final UUID plannedItemId = UUID.randomUUID();
    final UUID prescribedItemId = UUID.randomUUID();
    final UUID foodId = UUID.randomUUID();
    final UUID portionId = UUID.randomUUID();
    final Map<UUID, FoodPortion> available = new java.util.HashMap<>();

    {
        when(references.activePortions(org.mockito.ArgumentMatchers.anyCollection())).thenAnswer(invocation -> {
            java.util.Collection<UUID> ids = invocation.getArgument(0);
            var found = new java.util.HashMap<UUID, FoodPortion>();
            ids.stream().filter(available::containsKey).forEach(id -> found.put(id, available.get(id)));
            return found;
        });
    }

    FoodPortion portion(
            UUID food, UUID id, String quantity, String unit, String grams, String kcal
    ) {
        Map<String, NutrientValue> nutrients = kcal == null ? Map.of()
                : Map.of(
                        "energy_kcal",
                        new NutrientValue(new BigDecimal(kcal), "kcal", NutrientValueStatus.VALUE, null)
                );
        return new FoodPortion(
                id, food, "portion", new BigDecimal(quantity), unit,
                grams == null ? null : new BigDecimal(grams), true, null, null, null, null, nutrients
        );
    }

    FoodLog log(
            PlanningMode mode, String plannedQuantity, String plannedUnit, UUID plannedPortion,
            List<SubstitutionSource> substitutions
    ) {
        var item = new PlannedMealItem(
                plannedItemId, prescribedItemId, null, foodId, plannedPortion,
                new BigDecimal(plannedQuantity), plannedUnit, 0
        );
        var meal = new PlannedMeal(
                mealId, null, LocalDate.parse("2026-10-13"), null, MealType.LUNCH, "Lunch", 0,
                List.of(item)
        );
        var plan = WeeklyPlan.create(
                patient, versionId, mode, LocalDate.parse("2026-10-12"),
                java.time.Instant.parse("2026-10-12T00:00:00Z"), List.of(meal)
        );
        var source = new PrescriptionPlanningSource(
                versionId, mode,
                List.of(
                        new MealSource(
                                UUID.randomUUID(), "Lunch", MealType.LUNCH, 0, null,
                                List.of(
                                        new ItemSource(
                                                prescribedItemId, foodId, plannedPortion,
                                                new BigDecimal(plannedQuantity), plannedUnit, 0,
                                                substitutions
                                        )
                                )
                        )
                )
        );
        when(references.patientId(account)).thenReturn(Optional.of(patient));
        when(references.planForMeal(mealId)).thenReturn(Optional.of(plan));
        when(references.prescriptionVersion(versionId)).thenReturn(Optional.of(source));
        return useCase.get(new Actor(account, "PATIENT"), mealId);
    }

    @Test
    void scalesKcalAndGramsByQuantityOverPortionQuantity() {
        available.put(portionId, portion(foodId, portionId, "3", "g", "30", "100"));

        var item = log(PlanningMode.RESTRICTED, "1", "g", portionId, List.of()).items().getFirst();

        assertEquals("33.3333333333", item.kcalSnapshot().toString());
        assertEquals("9.99999999999", item.canonicalConversion().grams().toString());
        assertEquals(new BigDecimal("1"), item.quantity());
        assertEquals(BigDecimal.ONE, item.multiplier());
    }

    @Test
    void exactRatioKeepsTheTrimmedValues() {
        available.put(portionId, portion(foodId, portionId, "100", "g", "100", "130"));

        var item = log(PlanningMode.RESTRICTED, "250", "g", portionId, List.of()).items().getFirst();

        assertEquals("325", item.kcalSnapshot().toString());
        assertEquals("2.5E+2", item.canonicalConversion().grams().toString());
    }

    @Test
    void usesTheQuantityAsGramsWhenThePortionHasNoGramEquivalent() {
        available.put(portionId, portion(foodId, portionId, "100", "g", null, null));

        var item = log(PlanningMode.RESTRICTED, "80", "g", portionId, List.of()).items().getFirst();

        assertEquals("80", item.canonicalConversion().grams().toString());
        assertNull(item.kcalSnapshot());
    }

    @Test
    void leavesSnapshotsEmptyWithoutAnActivePortion() {
        var item = log(PlanningMode.RESTRICTED, "2", "unit", null, List.of()).items().getFirst();

        assertNull(item.canonicalConversion());
        assertNull(item.kcalSnapshot());

        var grams = log(PlanningMode.RESTRICTED, "2", "g", null, List.of()).items().getFirst();
        assertEquals("2", grams.canonicalConversion().grams().toString());
    }

    @Test
    void scalesAuthorizedSubstitutionsTheSameWay() {
        var substituteFood = UUID.randomUUID();
        var substitutePortion = UUID.randomUUID();
        var substitutionId = UUID.randomUUID();
        available.put(portionId, portion(foodId, portionId, "100", "g", "100", "100"));
        available.put(substitutePortion, portion(substituteFood, substitutePortion, "7", "g", "21", "50"));
        var substitution = new SubstitutionSource(
                substitutionId, substituteFood, substitutePortion, new BigDecimal("2"), "g"
        );

        var item = log(PlanningMode.SUBSTITUTIONS_ONLY, "100", "g", portionId, List.of(substitution))
                .items().getFirst();

        var sub = item.allowedSubstitutions().getFirst();
        assertEquals(substitutionId, sub.allowedSubstitutionId());
        assertEquals("14.2857142857", sub.kcalSnapshot().toString());
        assertEquals("5.999999999994", sub.canonicalConversion().grams().toString());
    }
}

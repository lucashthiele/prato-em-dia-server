package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.Actor;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PatientQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.AllowedSubstitution;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescriptionVersion;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrescriptionSummaryUseCaseTest {
    final PrescriptionRepository repository = mock(PrescriptionRepository.class);
    final PrescriptionReferences references = mock(PrescriptionReferences.class);
    final PrescriptionSummaryUseCase useCase = new PrescriptionSummaryUseCase(repository, references);
    final UUID account = UUID.randomUUID();
    final UUID nutritionist = UUID.randomUUID();
    final UUID patient = UUID.randomUUID();
    final Actor actor = new Actor(account, "NUTRITIONIST");

    Food food(String name) {
        return Food.create(name, "cat", "g", null, null, account, FoodOrigin.NUTRITIONIST);
    }

    FoodPortion portion(Food food, String label, Map<String, String> macros) {
        var nutrients = new java.util.HashMap<String, NutrientValue>();
        macros.forEach(
                (code, amount) -> nutrients
                        .put(code, new NutrientValue(new BigDecimal(amount), "g", NutrientValueStatus.VALUE, null))
        );
        return new FoodPortion(
                UUID.randomUUID(), food.getId(), label, new BigDecimal("100"), "g", new BigDecimal("100"),
                true, null, null, null, null, nutrients
        );
    }

    PrescribedMealItem item(Food food, FoodPortion portion, String quantity, int order, AllowedSubstitution... subs) {
        return PrescribedMealItem.create(
                food.getId(), portion == null ? null : portion.getId(), new BigDecimal(quantity), "g",
                order, "note", List.of(subs)
        );
    }

    PrescriptionVersion version(
            PrescriptionVersionStatus status, int number, List<PrescribedMeal> meals
    ) {
        return new PrescriptionVersion(
                UUID.randomUUID(), number, status, "Title " + number, "Notes", account,
                Instant.parse("2026-10-01T00:00:00Z"),
                status == PrescriptionVersionStatus.DRAFT ? null : Instant.parse("2026-10-02T00:00:00Z"),
                status == PrescriptionVersionStatus.ARCHIVED ? Instant.parse("2026-10-03T00:00:00Z") : null,
                meals
        );
    }

    PrescribedMeal meal(MealType type, int order, PrescribedMealItem... items) {
        return PrescribedMeal.create(type.name(), type, order, LocalTime.of(8, 0), null, List.of(items));
    }

    void stubCatalog(List<Food> foods, List<FoodPortion> portions) {
        when(references.foods(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(
                        foods.stream().collect(java.util.stream.Collectors.toMap(Food::getId, f -> f))
                );
        when(references.portions(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(
                        portions.stream()
                                .collect(java.util.stream.Collectors.toMap(FoodPortion::getId, p -> p))
                );
    }

    void givenPrescription(PrescriptionVersion... versions) {
        when(references.nutritionistProfileId(account)).thenReturn(Optional.of(nutritionist));
        when(references.hasActiveRelationship(nutritionist, patient)).thenReturn(true);
        when(repository.findByNutritionistAndPatient(nutritionist, patient)).thenReturn(
                List.of(
                        new Prescription(
                                UUID.randomUUID(), patient, nutritionist, PrescriptionStatus.ACTIVE,
                                PlanningMode.SUBSTITUTIONS_ONLY, Instant.now(), Instant.now(), 0,
                                List.of(versions)
                        )
                )
        );
    }

    @Test
    void withoutPrescriptionReturnsAnEmptySummary() {
        when(references.nutritionistProfileId(account)).thenReturn(Optional.of(nutritionist));
        when(references.hasActiveRelationship(nutritionist, patient)).thenReturn(true);
        when(repository.findByNutritionistAndPatient(nutritionist, patient)).thenReturn(List.of());

        var summary = useCase.summary(new PatientQuery(actor, patient));

        assertNull(summary.prescriptionId());
        assertNull(summary.current());
        assertNull(summary.draft());
        assertTrue(summary.history().isEmpty());
    }

    @Test
    void scalesMacrosByQuantityAndSumsMainItemsOnly() {
        var rice = food("Rice");
        var riceP = portion(
                rice, "100 g", Map.of("energy_kcal", "130", "protein", "2.5", "carbohydrate", "28", "lipids", "0.3")
        );
        var bean = food("Bean");
        var beanP = portion(
                bean, "100 g", Map.of("energy_kcal", "77", "protein", "4.5", "carbohydrate", "14", "lipids", "0.5")
        );
        var quinoa = food("Quinoa");
        var quinoaP = portion(
                quinoa, "100 g", Map.of("energy_kcal", "999", "protein", "99", "carbohydrate", "99", "lipids", "99")
        );
        var substitution = AllowedSubstitution
                .create(quinoa.getId(), quinoaP.getId(), new BigDecimal("50"), "g", "alt");
        var lunch = meal(
                MealType.LUNCH, 0, item(rice, riceP, "200", 0, substitution), item(bean, beanP, "50", 1)
        );
        var published = version(PrescriptionVersionStatus.PUBLISHED, 1, List.of(lunch));
        givenPrescription(published);
        stubCatalog(List.of(rice, bean, quinoa), List.of(riceP, beanP, quinoaP));

        var summary = useCase.summary(new PatientQuery(actor, patient));

        var meal = summary.current().meals().getFirst();
        var riceItem = meal.items().get(0);
        assertEquals("Rice", riceItem.foodName());
        assertEquals("100 g", riceItem.portionLabel());
        assertEquals(0, new BigDecimal("260").compareTo(riceItem.nutrients().energyKcal()));
        assertEquals(0, new BigDecimal("5").compareTo(riceItem.nutrients().protein()));
        assertTrue(riceItem.nutrients().complete());
        assertEquals("Quinoa", riceItem.substitutions().getFirst().foodName());
        assertEquals(0, new BigDecimal("298.5").compareTo(meal.totals().energyKcal()));
        assertEquals(0, new BigDecimal("7.25").compareTo(meal.totals().protein()));
        assertEquals(0, new BigDecimal("63").compareTo(meal.totals().carbohydrate()));
        assertEquals(0, new BigDecimal("0.85").compareTo(meal.totals().lipids()));
        assertTrue(meal.totals().complete());
        assertEquals(meal.totals(), summary.current().totals());
        assertEquals(LocalTime.of(8, 0), meal.suggestedTime());
    }

    @Test
    void itemWithoutPortionOrNutrientMakesTotalsIncomplete() {
        var rice = food("Rice");
        var riceP = portion(rice, "100 g", Map.of("energy_kcal", "100", "protein", "10"));
        var apple = food("Apple");
        var published = version(
                PrescriptionVersionStatus.PUBLISHED, 1,
                List.of(
                        meal(
                                MealType.LUNCH, 0, item(rice, riceP, "100", 0), item(apple, null, "1", 1)
                        )
                )
        );
        givenPrescription(published);
        stubCatalog(List.of(rice, apple), List.of(riceP));

        var summary = useCase.summary(new PatientQuery(actor, patient));

        var items = summary.current().meals().getFirst().items();
        assertNull(items.get(0).nutrients().carbohydrate());
        assertFalse(items.get(0).nutrients().complete());
        assertNull(items.get(1).portionLabel());
        assertNull(items.get(1).nutrients().energyKcal());
        assertFalse(items.get(1).nutrients().complete());
        var totals = summary.current().totals();
        assertEquals(0, new BigDecimal("100").compareTo(totals.energyKcal()));
        assertFalse(totals.complete());
    }

    @Test
    void splitsCurrentDraftAndHistory() {
        var archived = version(PrescriptionVersionStatus.ARCHIVED, 1, List.of());
        var published = version(PrescriptionVersionStatus.PUBLISHED, 2, List.of());
        var draft = version(PrescriptionVersionStatus.DRAFT, 3, List.of());
        givenPrescription(archived, published, draft);
        stubCatalog(List.of(), List.of());

        var summary = useCase.summary(new PatientQuery(actor, patient));

        assertEquals(published.getId(), summary.current().id());
        assertEquals(draft.getId(), summary.draft().id());
        assertEquals(PlanningMode.SUBSTITUTIONS_ONLY, summary.planningMode());
        assertEquals(List.of(3, 2, 1), summary.history().stream().map(h -> h.number()).toList());
        assertEquals(Instant.parse("2026-10-03T00:00:00Z"), summary.history().get(2).archivedAt());
    }

    @Test
    void requiresAnActiveRelationship() {
        when(references.nutritionistProfileId(account)).thenReturn(Optional.of(nutritionist));
        when(references.hasActiveRelationship(nutritionist, patient)).thenReturn(false);

        assertThrows(
                PrescriptionAccessDeniedException.class,
                () -> useCase.summary(new PatientQuery(actor, patient))
        );
    }

    @Test
    void rejectsNonNutritionists() {
        assertThrows(
                PrescriptionAccessDeniedException.class,
                () -> useCase.summary(new PatientQuery(new Actor(account, "PATIENT"), patient))
        );
    }
}

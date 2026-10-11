package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Create;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.ConsumedItem;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.TrackingReferences;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import com.ulvratech.pratoemdia.server.features.tracking.domain.port.out.MealCompletionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TrackingUseCaseTest {
    private final UUID account = UUID.randomUUID();
    private final UUID patient = UUID.randomUUID();
    private final UUID version = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID prescribedItem = UUID.randomUUID();
    private final UUID substitution = UUID.randomUUID();
    private final UUID plannedFood = UUID.randomUUID();
    private final UUID plannedPortion = UUID.randomUUID();
    private final UUID substituteFood = UUID.randomUUID();
    private final UUID substitutePortion = UUID.randomUUID();
    private final PlannedMeal meal = new PlannedMeal(
            UUID.randomUUID(), UUID.randomUUID(),
            LocalDate.of(2026, 9, 18), LocalTime.NOON, MealType.LUNCH, "Lunch", 0,
            List.of(
                    new PlannedMealItem(
                            itemId, prescribedItem, null, plannedFood, plannedPortion,
                            BigDecimal.ONE, "g", 0
                    )
            )
    );
    private final WeeklyPlan plan = new WeeklyPlan(
            UUID.randomUUID(), patient, version,
            PlanningMode.SUBSTITUTIONS_ONLY, LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 24),
            Instant.parse("2026-09-18T03:00:00Z"), WeeklyPlanStatus.PENDING, null,
            null, null, 0, List.of(meal)
    );
    private final Instant occurred = Instant.parse("2026-09-18T15:00:00Z");

    @Test
    void substitutionUsesPrescriptionVersionAndPreservesOriginalItem() {
        var fixture = fixture();
        var source = new PrescriptionPlanningSource(
                version, PlanningMode.SUBSTITUTIONS_ONLY, List.of(
                        new PrescriptionPlanningSource.MealSource(
                                meal.prescribedMealId(), "Lunch",
                                MealType.LUNCH, 0, LocalTime.NOON, List.of(
                                        new PrescriptionPlanningSource.ItemSource(
                                                prescribedItem, UUID.randomUUID(),
                                                null, BigDecimal.ONE, "g", 0, List.of(
                                                        new PrescriptionPlanningSource.SubstitutionSource(
                                                                substitution,
                                                                substituteFood, substitutePortion,
                                                                BigDecimal.TEN, "g"
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
        when(fixture.references.prescriptionVersion(version)).thenReturn(Optional.of(source));
        var result = fixture.useCase.create(
                command(substituteFood, substitutePortion, BigDecimal.TEN, substitution)
        );
        var json = new ObjectMapper().findAndRegisterModules();
        assertEquals(
                substituteFood.toString(),
                json.valueToTree(result.consumedItemsSnapshot()).get(0).get("catalogFoodId").asText()
        );
        assertEquals(
                meal.items().getFirst().foodId().toString(),
                json.valueToTree(result.plannedItemsSnapshot()).get(0).get("catalogFoodId").asText()
        );
    }

    @Test
    void sameKeyReturnsOriginalAndDifferentContentConflicts() {
        var fixture = fixture();
        var first = fixture.useCase.create(command(plannedFood, plannedPortion, BigDecimal.ONE, null));
        assertEquals(
                first.id(), fixture.useCase.create(
                        command(plannedFood, plannedPortion, BigDecimal.ONE, null)
                ).id()
        );
        assertEquals(
                TrackingException.Kind.CONFLICT,
                assertThrows(
                        TrackingException.class, () -> fixture.useCase.create(
                                command(plannedFood, plannedPortion, BigDecimal.TWO, null)
                        )
                ).kind()
        );
    }

    @Test
    void rejectsFutureAndOutOfPeriodTimes() {
        var fixture = fixture();
        assertEquals(
                TrackingException.Kind.INVALID,
                assertThrows(
                        TrackingException.class, () -> fixture.useCase.create(
                                new Create(
                                        new Actor(account, "PATIENT"), meal.id(), "key", null,
                                        Instant.parse("2026-09-20T00:00:00Z"), List.of()
                                )
                        )
                ).kind()
        );
        assertEquals(
                TrackingException.Kind.INVALID,
                assertThrows(
                        TrackingException.class, () -> fixture.useCase.create(
                                new Create(
                                        new Actor(account, "PATIENT"), meal.id(), "key", null,
                                        Instant.parse("2026-09-18T02:59:59Z"), List.of()
                                )
                        )
                ).kind()
        );
    }

    @Test
    void summaryIncludesRemovedMealAndUsesLastCreatedCorrection() {
        var fixture = fixture();
        var removed = UUID.randomUUID();
        var old = event(1, removed, Type.COMPLETED);
        var correction = event(2, removed, Type.NOT_COMPLETED);
        when(fixture.references.plan(plan.getId())).thenReturn(Optional.of(plan));
        when(fixture.references.hasActiveRelationship(account, patient)).thenReturn(true);
        when(fixture.repository.forPlan(plan.getId())).thenReturn(List.of(old, correction));
        var summary = fixture.useCase.summary(new Actor(account, "NUTRITIONIST"), patient, plan.getId());
        assertEquals(2, summary.total());
        assertEquals(1, summary.notCompleted());
        assertEquals(1, summary.unrecorded());
    }

    private Create command(
            UUID foodId, UUID portionId, BigDecimal quantity, UUID allowedSubstitutionId
    ) {
        return new Create(
                new Actor(account, "PATIENT"), meal.id(), "key", null, occurred,
                List.of(
                        new ConsumedItem(
                                foodId, portionId, "g", quantity, BigDecimal.ONE, itemId,
                                allowedSubstitutionId
                        )
                )
        );
    }

    private MealCompletionEvent event(long sequence, UUID mealId, Type type) {
        return new MealCompletionEvent(
                sequence, UUID.randomUUID(), patient, plan.getId(), mealId,
                type, null, occurred, occurred, "key" + sequence, "hash", "{}", "[]", "[]"
        );
    }

    private Fixture fixture() {
        var repository = mock(MealCompletionRepository.class);
        var references = mock(TrackingReferences.class);
        var saved = new AtomicReference<MealCompletionEvent>();
        when(references.patientId(account)).thenReturn(Optional.of(patient));
        when(references.planForMeal(meal.id())).thenReturn(Optional.of(plan));
        when(references.prescriptionVersion(version)).thenReturn(Optional.of(source()));
        when(references.activeFood(any())).thenReturn(
                Optional.of(
                        mock(
                                com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food.class
                        )
                )
        );
        var portion = mock(com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion.class);
        when(portion.getUnit()).thenReturn("g");
        when(portion.getQuantity()).thenReturn(BigDecimal.ONE);
        when(portion.getGramsEquivalent()).thenReturn(null);
        when(portion.getNutrients()).thenReturn(java.util.Map.of());
        when(references.activePortion(any(), any())).thenReturn(Optional.of(portion));
        when(repository.byKey(patient, "key")).thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        when(repository.append(any())).thenAnswer(invocation -> {
            var input = (MealCompletionEvent) invocation.getArgument(0);
            var value = new MealCompletionEvent(
                    1, input.id(), input.patientId(), input.weeklyPlanId(),
                    input.plannedMealId(), input.eventType(), input.reason(), input.occurredAt(),
                    input.createdAt(), input.idempotencyKey(), input.commandHash(),
                    input.mealSnapshot(), input.plannedItemsSnapshot(), input.consumedItemsSnapshot()
            );
            saved.set(value);
            return value;
        });
        return new Fixture(
                new TrackingUseCase(
                        repository, references, new ObjectMapper().findAndRegisterModules(),
                        Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC)
                ), repository, references
        );
    }

    private PrescriptionPlanningSource source() {
        return new PrescriptionPlanningSource(
                version, PlanningMode.SUBSTITUTIONS_ONLY, List.of(
                        new PrescriptionPlanningSource.MealSource(
                                meal.prescribedMealId(), "Lunch", MealType.LUNCH, 0, LocalTime.NOON,
                                List.of(
                                        new PrescriptionPlanningSource.ItemSource(
                                                prescribedItem, plannedFood, plannedPortion,
                                                BigDecimal.ONE, "g", 0, List.of(
                                                        new PrescriptionPlanningSource.SubstitutionSource(
                                                                substitution, substituteFood, substitutePortion,
                                                                BigDecimal.TEN, "g"
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private record Fixture(TrackingUseCase useCase, MealCompletionRepository repository,
            TrackingReferences references) {
    }
}

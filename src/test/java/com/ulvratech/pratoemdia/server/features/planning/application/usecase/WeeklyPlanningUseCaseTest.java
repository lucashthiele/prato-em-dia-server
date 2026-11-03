package com.ulvratech.pratoemdia.server.features.planning.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.ItemSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.MealSource;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanGenerationConflictException;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import org.springframework.dao.DataIntegrityViolationException;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanGeneratedNotifier;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlanningPreference;
import com.ulvratech.pratoemdia.server.features.planning.domain.port.out.WeeklyPlanningRepository;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WeeklyPlanningUseCaseTest {
    private static final Clock THURSDAY_9AM = Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void manualGenerationNotifiesOnceAsManual() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var user = UUID.randomUUID();
        var patient = UUID.randomUUID();
        when(references.patientProfileId(user)).thenReturn(Optional.of(patient));
        when(references.currentPublishedPrescription(patient))
                .thenReturn(Optional.of(source(UUID.randomUUID(), PlanningMode.FREE)));
        when(repository.savePlan(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);

        useCase.generate(new Actor(user, "PATIENT"));

        var notice = ArgumentCaptor.forClass(PlanGeneratedNotice.class);
        verify(notifier, times(1)).planGenerated(notice.capture());
        assertEquals(patient, notice.getValue().patientId());
        assertFalse(notice.getValue().automatic());
        assertEquals(5, notice.getValue().mealCount());
        assertEquals(1, notice.getValue().mealsPerDay());
        assertEquals(List.of(MealType.LUNCH), notice.getValue().dailyMealTypes());
        assertEquals(LocalDate.of(2026, 9, 17), notice.getValue().periodStart());
        assertEquals(LocalDate.of(2026, 9, 21), notice.getValue().periodEnd());
    }

    @Test
    void manualRegenerationWithoutPreferenceCoversTodayUpToTheDefaultMonday() {
        var fixture = manualFixture();

        var output = fixture.useCase.generate(fixture.actor);

        assertEquals(LocalDate.of(2026, 9, 17), output.periodStart());
        assertEquals(LocalDate.of(2026, 9, 21), output.periodEnd());
        assertEquals(5, output.meals().size());
        assertEquals(PlanningMode.SUBSTITUTIONS_ONLY, output.planningMode());
        assertEquals(fixture.version, output.prescriptionVersionId());
        assertEquals(Instant.parse("2026-09-18T03:00:00Z"), output.confirmationDeadline());
    }

    @Test
    void manualRegenerationOnTheAutomaticDayCoversOnlyThatDay() {
        var fixture = manualFixture();
        when(fixture.repository.findPreference(fixture.patient))
                .thenReturn(Optional.of(new WeeklyPlanningPreference(fixture.patient, DayOfWeek.THURSDAY, true)));

        var output = fixture.useCase.generate(fixture.actor);

        assertEquals(LocalDate.of(2026, 9, 17), output.periodStart());
        assertEquals(LocalDate.of(2026, 9, 17), output.periodEnd());
        assertEquals(1, output.meals().size());
    }

    @Test
    void manualRegenerationSupersedesOnlyTheCoveredDaysOfTheOlderPlanBeforeSavingTheNewOne() {
        var fixture = manualFixture();
        var older = olderPlan(fixture.patient, LocalDate.of(2026, 9, 15), 10);
        when(
                fixture.repository.findPlansWithActiveMeals(
                        fixture.patient, LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 21)
                )
        ).thenReturn(List.of(older));

        var output = fixture.useCase.generate(fixture.actor);

        assertEquals(
                List.of(
                        LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 22),
                        LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 24)
                ),
                older.getMeals().stream().map(meal -> meal.date()).sorted().toList()
        );
        var order = inOrder(fixture.repository);
        order.verify(fixture.repository).savePlan(older);
        order.verify(fixture.repository).savePlan(argThat(plan -> plan.getId().equals(output.id())));
    }

    @Test
    void concurrentManualGenerationLosingTheRaceGetsACodedConflictAndNoNotification() {
        var fixture = manualFixture();
        when(fixture.repository.savePlan(any()))
                .thenThrow(new DataIntegrityViolationException("ux_planned_meal_active_slot"));

        var error = assertThrows(PlanGenerationConflictException.class, () -> fixture.useCase.generate(fixture.actor));

        assertEquals("WEEKLY_PLAN_GENERATION_CONFLICT", error.getCode());
        verify(fixture.notifier, never()).planGenerated(any());
    }

    @Test
    void automaticGenerationCoversTheNextSevenDaysAndNotifiesOnce() {
        var fixture = automaticFixture();

        fixture.useCase.generateDuePlans();

        var saved = ArgumentCaptor.forClass(WeeklyPlan.class);
        verify(fixture.repository).savePlan(saved.capture());
        assertEquals(LocalDate.of(2026, 9, 18), saved.getValue().getPeriodStart());
        assertEquals(LocalDate.of(2026, 9, 24), saved.getValue().getPeriodEnd());
        assertEquals(7, saved.getValue().getMeals().size());
        verify(fixture.notifier, times(1)).planGenerated(any());
    }

    @Test
    void automaticGenerationFillsOnlyTheDaysWithoutActiveMeals() {
        var fixture = automaticFixture();
        var existing = olderPlan(fixture.patient, LocalDate.of(2026, 9, 18), 3);
        when(
                fixture.repository.findPlansWithActiveMeals(
                        fixture.patient, LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 24)
                )
        ).thenReturn(List.of(existing));

        fixture.useCase.generateDuePlans();

        var saved = ArgumentCaptor.forClass(WeeklyPlan.class);
        verify(fixture.repository).savePlan(saved.capture());
        assertEquals(LocalDate.of(2026, 9, 21), saved.getValue().getPeriodStart());
        assertEquals(LocalDate.of(2026, 9, 24), saved.getValue().getPeriodEnd());
        assertEquals(4, saved.getValue().getMeals().size());
        assertEquals(3, existing.getMeals().size());
    }

    @Test
    void automaticGenerationIsIdempotentWhenEveryDayAlreadyHasActiveMeals() {
        var fixture = automaticFixture();
        var existing = olderPlan(fixture.patient, LocalDate.of(2026, 9, 18), 7);
        when(
                fixture.repository.findPlansWithActiveMeals(
                        fixture.patient, LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 24)
                )
        ).thenReturn(List.of(existing));

        fixture.useCase.generateDuePlans();
        fixture.useCase.generateDuePlans();

        verify(fixture.repository, never()).savePlan(any());
        verify(fixture.notifier, never()).planGenerated(any());
    }

    @Test
    void automaticGenerationLosingTheRaceReturnsThePlanAlreadyGeneratedWithoutNotifying() {
        var fixture = automaticFixture();
        var winner = olderPlan(fixture.patient, LocalDate.of(2026, 9, 18), 7);
        when(
                fixture.repository.findPlansWithActiveMeals(
                        fixture.patient, LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 24)
                )
        ).thenReturn(List.of(), List.of(winner));
        when(fixture.repository.savePlan(any()))
                .thenThrow(new DataIntegrityViolationException("ux_planned_meal_active_slot"));

        fixture.useCase.generateDuePlans();

        verify(fixture.notifier, never()).planGenerated(any());
    }

    private record Fixture(
            WeeklyPlanningRepository repository,
            PlanGeneratedNotifier notifier,
            WeeklyPlanningUseCase useCase,
            Actor actor,
            UUID patient,
            UUID version) {
    }

    private Fixture manualFixture() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var user = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var version = UUID.randomUUID();
        when(references.patientProfileId(user)).thenReturn(Optional.of(patient));
        when(references.currentPublishedPrescription(patient))
                .thenReturn(Optional.of(source(version, PlanningMode.SUBSTITUTIONS_ONLY)));
        when(repository.savePlan(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);
        return new Fixture(repository, notifier, useCase, new Actor(user, "PATIENT"), patient, version);
    }

    private Fixture automaticFixture() {
        var fixture = manualFixture();
        when(fixture.repository.findEnabledPreferences(DayOfWeek.THURSDAY))
                .thenReturn(List.of(new WeeklyPlanningPreference(fixture.patient, DayOfWeek.THURSDAY, true)));
        return fixture;
    }

    private WeeklyPlan olderPlan(UUID patient, LocalDate start, int days) {
        var meals = new java.util.ArrayList<PlannedMeal>();
        for (int day = 0; day < days; day++)
            meals.add(
                    new PlannedMeal(UUID.randomUUID(), null, start.plusDays(day), MealType.LUNCH, "Lunch", 0, List.of())
            );
        return WeeklyPlan.create(
                patient, UUID.randomUUID(), PlanningMode.FREE, start, start.plusDays(days - 1),
                start.atStartOfDay(WeeklyPlanningUseCase.ZONE).toInstant(), meals
        );
    }

    @Test
    void scheduledGenerationNotifiesAsAutomatic() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var patient = UUID.randomUUID();
        when(repository.findEnabledPreferences(DayOfWeek.THURSDAY))
                .thenReturn(List.of(new WeeklyPlanningPreference(patient, DayOfWeek.THURSDAY, true)));
        when(references.currentPublishedPrescription(patient))
                .thenReturn(Optional.of(source(UUID.randomUUID(), PlanningMode.FREE)));
        when(repository.savePlan(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);

        useCase.generateDuePlans();

        var notice = ArgumentCaptor.forClass(PlanGeneratedNotice.class);
        verify(notifier, times(1)).planGenerated(notice.capture());
        assertTrue(notice.getValue().automatic());
        assertEquals(patient, notice.getValue().patientId());
    }

    @Test
    void scheduledGenerationDoesNothingBefore0800() {
        var repository = mock(WeeklyPlanningRepository.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var earlyClock = Clock.fixed(Instant.parse("2026-09-17T10:59:00Z"), ZoneOffset.UTC);
        var useCase = new WeeklyPlanningUseCase(
                repository, mock(PlanningReferences.class), earlyClock, notifier
        );

        useCase.generateDuePlans();

        verify(repository, never()).findEnabledPreferences(any());
    }

    @Test
    void scheduledGenerationRejectedByValidationIsNotRepeatedAndEmailsThePatient() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var rejected = UUID.randomUUID();
        var fine = UUID.randomUUID();
        when(repository.findEnabledPreferences(DayOfWeek.THURSDAY))
                .thenReturn(
                        List.of(
                                new WeeklyPlanningPreference(rejected, DayOfWeek.THURSDAY, true),
                                new WeeklyPlanningPreference(fine, DayOfWeek.THURSDAY, true)
                        )
                );
        when(references.currentPublishedPrescription(rejected)).thenThrow(new IllegalStateException("rule"));
        when(references.currentPublishedPrescription(fine))
                .thenReturn(Optional.of(source(UUID.randomUUID(), PlanningMode.FREE)));
        when(repository.savePlan(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);

        useCase.generateDuePlans();

        verify(notifier).planGenerationRejected(rejected);
        verify(notifier, times(1)).planGenerated(any());
    }

    @Test
    void notifierFailureDoesNotFailGeneration() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var user = UUID.randomUUID();
        var patient = UUID.randomUUID();
        when(references.patientProfileId(user)).thenReturn(Optional.of(patient));
        when(references.currentPublishedPrescription(patient))
                .thenReturn(Optional.of(source(UUID.randomUUID(), PlanningMode.FREE)));
        when(repository.savePlan(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("boom")).when(notifier).planGenerated(any());
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);

        var output = useCase.generate(new Actor(user, "PATIENT"));

        assertEquals(5, output.meals().size());
    }

    @Test
    void deleteDeactivatesOwnedPlan() {
        var fixture = deleteFixture();

        fixture.useCase.delete(new PlanQuery(fixture.actor, fixture.plan.getId()));

        verify(fixture.repository).deactivatePlan(fixture.plan.getId());
    }

    @Test
    void deleteOfAnotherPatientsPlanIsDenied() {
        var fixture = deleteFixture();
        var other = new Actor(UUID.randomUUID(), "PATIENT");
        when(fixture.references.patientProfileId(other.userAccountId()))
                .thenReturn(Optional.of(UUID.randomUUID()));

        assertThrows(
                PlanningAccessDeniedException.class,
                () -> fixture.useCase.delete(new PlanQuery(other, fixture.plan.getId()))
        );
        verify(fixture.repository, never()).deactivatePlan(any());
    }

    private record DeleteFixture(
            WeeklyPlanningRepository repository,
            PlanningReferences references,
            WeeklyPlanningUseCase useCase,
            Actor actor,
            WeeklyPlan plan) {
    }

    private DeleteFixture deleteFixture() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var actor = new Actor(UUID.randomUUID(), "PATIENT");
        var patient = UUID.randomUUID();
        var plan = WeeklyPlan.create(
                patient,
                UUID.randomUUID(),
                PlanningMode.FREE,
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 24),
                Instant.parse("2026-09-18T03:00:00Z"),
                List.of()
        );
        when(references.patientProfileId(actor.userAccountId())).thenReturn(Optional.of(patient));
        when(repository.findPlan(plan.getId())).thenReturn(Optional.of(plan));
        var useCase = new WeeklyPlanningUseCase(
                repository,
                references,
                Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC),
                mock(PlanGeneratedNotifier.class)
        );
        return new DeleteFixture(repository, references, useCase, actor, plan);
    }

    private PrescriptionPlanningSource source(UUID version, PlanningMode mode) {
        var item = new ItemSource(
                UUID.randomUUID(), UUID.randomUUID(), null, BigDecimal.ONE, "g", 0, List.of()
        );
        var meal = new MealSource(
                UUID.randomUUID(), "Lunch", MealType.LUNCH, 0, LocalTime.NOON, List.of(item)
        );
        return new PrescriptionPlanningSource(version, mode, List.of(meal));
    }
}

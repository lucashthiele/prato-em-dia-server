package com.ulvratech.pratoemdia.server.features.planning.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
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
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningConflictException;
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
    @Test
    void manuallyGeneratesSevenFollowingDaysAndSnapshotsMode() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var user = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var version = UUID.randomUUID();
        var source = source(version, PlanningMode.SUBSTITUTIONS_ONLY);
        when(references.patientProfileId(user)).thenReturn(Optional.of(patient));
        when(references.currentPublishedPrescription(patient)).thenReturn(Optional.of(source));
        when(repository.savePlan(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var clock = Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);
        var useCase = new WeeklyPlanningUseCase(repository, references, clock, mock(PlanGeneratedNotifier.class));

        var output = useCase.generate(new Actor(user, "PATIENT"));

        assertEquals(LocalDate.of(2026, 9, 18), output.periodStart());
        assertEquals(LocalDate.of(2026, 9, 24), output.periodEnd());
        assertEquals(7, output.meals().size());
        assertEquals(PlanningMode.SUBSTITUTIONS_ONLY, output.planningMode());
        assertEquals(version, output.prescriptionVersionId());
    }

    @Test
    void duplicateManualPeriodIsAConflict() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var user = UUID.randomUUID();
        var patient = UUID.randomUUID();
        when(references.patientProfileId(user)).thenReturn(Optional.of(patient));
        when(repository.existsForPeriod(eq(patient), any(), any())).thenReturn(true);
        var useCase = new WeeklyPlanningUseCase(
                repository,
                references,
                Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC),
                mock(PlanGeneratedNotifier.class)
        );
        assertThrows(
                PlanningConflictException.class, () -> useCase.generate(new Actor(user, "PATIENT"))
        );
        verify(references, never()).currentPublishedPrescription(any());
    }

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
        assertEquals(7, notice.getValue().mealCount());
        assertEquals(1, notice.getValue().mealsPerDay());
        assertEquals(List.of(MealType.LUNCH), notice.getValue().dailyMealTypes());
        assertEquals(LocalDate.of(2026, 9, 18), notice.getValue().periodStart());
        assertEquals(LocalDate.of(2026, 9, 24), notice.getValue().periodEnd());
    }

    @Test
    void conflictingManualGenerationDoesNotNotify() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var user = UUID.randomUUID();
        var patient = UUID.randomUUID();
        when(references.patientProfileId(user)).thenReturn(Optional.of(patient));
        when(repository.existsForPeriod(eq(patient), any(), any())).thenReturn(true);
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);

        assertThrows(PlanningConflictException.class, () -> useCase.generate(new Actor(user, "PATIENT")));

        verify(notifier, never()).planGenerated(any());
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
    void scheduledGenerationDoesNotNotifyWhenPlanAlreadyExists() {
        var repository = mock(WeeklyPlanningRepository.class);
        var references = mock(PlanningReferences.class);
        var notifier = mock(PlanGeneratedNotifier.class);
        var patient = UUID.randomUUID();
        var existing = WeeklyPlan.create(
                patient,
                UUID.randomUUID(),
                PlanningMode.FREE,
                LocalDate.of(2026, 9, 18),
                Instant.parse("2026-09-18T03:00:00Z"),
                List.of()
        );
        when(repository.findEnabledPreferences(DayOfWeek.THURSDAY))
                .thenReturn(List.of(new WeeklyPlanningPreference(patient, DayOfWeek.THURSDAY, true)));
        when(repository.existsForPeriod(eq(patient), any(), any())).thenReturn(true);
        when(repository.findPlans(patient)).thenReturn(List.of(existing));
        var useCase = new WeeklyPlanningUseCase(repository, references, THURSDAY_9AM, notifier);

        useCase.generateDuePlans();

        verify(notifier, never()).planGenerated(any());
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

        assertEquals(7, output.meals().size());
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

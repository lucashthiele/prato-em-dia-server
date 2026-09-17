package com.ulvratech.pratoemdia.server.features.planning.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.ItemSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.MealSource;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningConflictException;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.planning.domain.port.out.WeeklyPlanningRepository;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
    var useCase = new WeeklyPlanningUseCase(repository, references, clock);

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
    var useCase =
        new WeeklyPlanningUseCase(
            repository,
            references,
            Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC));
    assertThrows(
        PlanningConflictException.class, () -> useCase.generate(new Actor(user, "PATIENT")));
    verify(references, never()).currentPublishedPrescription(any());
  }

  private PrescriptionPlanningSource source(UUID version, PlanningMode mode) {
    var item =
        new ItemSource(
            UUID.randomUUID(), UUID.randomUUID(), null, BigDecimal.ONE, "g", 0, List.of());
    var meal =
        new MealSource(
            UUID.randomUUID(), "Lunch", MealType.LUNCH, 0, LocalTime.NOON, List.of(item));
    return new PrescriptionPlanningSource(version, mode, List.of(meal));
  }
}

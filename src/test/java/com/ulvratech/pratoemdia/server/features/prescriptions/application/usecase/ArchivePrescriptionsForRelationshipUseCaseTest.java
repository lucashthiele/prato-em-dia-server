package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArchivePrescriptionsForRelationshipUseCaseTest {
    private final Instant now = Instant.parse("2026-11-16T12:00:00Z");
    private final PrescriptionRepository repository = mock(PrescriptionRepository.class);
    private final ArchivePrescriptionsForRelationshipUseCase useCase = new ArchivePrescriptionsForRelationshipUseCase(
            repository, Clock.fixed(now, ZoneOffset.UTC)
    );

    @Test
    void archivesAndSavesEveryActivePrescriptionOfThePair() {
        var nutritionist = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var prescription = prescription(patient, nutritionist);
        prescription.publish(prescription.getVersions().getFirst().getId(), now.minusSeconds(60));
        when(repository.findByNutritionistAndPatient(nutritionist, patient)).thenReturn(List.of(prescription));

        useCase.execute(nutritionist, patient);

        assertEquals(PrescriptionStatus.ARCHIVED, prescription.getStatus());
        assertEquals(now, prescription.getVersions().getFirst().getArchivedAt());
        verify(repository).save(prescription);
    }

    @Test
    void doesNothingWhenThePairHasNoActivePrescription() {
        when(repository.findByNutritionistAndPatient(UUID.randomUUID(), UUID.randomUUID())).thenReturn(List.of());

        useCase.execute(UUID.randomUUID(), UUID.randomUUID());

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static Prescription prescription(UUID patient, UUID nutritionist) {
        var item = PrescribedMealItem.create(UUID.randomUUID(), null, BigDecimal.ONE, "g", 0, null, List.of());
        var meal = PrescribedMeal.create("Breakfast", 0, null, null, List.of(item));
        return Prescription.create(patient, nutritionist, UUID.randomUUID(), "Plan", null, List.of(meal));
    }
}

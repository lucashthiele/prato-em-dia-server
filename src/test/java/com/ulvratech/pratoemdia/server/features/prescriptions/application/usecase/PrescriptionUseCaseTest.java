package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PrescriptionUseCaseTest {
    @Test
    void createsAndPublishesOnlyForTheLinkedAuthor() {
        var repository = mock(PrescriptionRepository.class);
        var references = mock(PrescriptionReferences.class);
        var nutritionistAccount = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var patientAccount = UUID.randomUUID();
        var food = UUID.randomUUID();
        var stored = new java.util.concurrent.atomic.AtomicReference<Prescription>();
        when(references.nutritionistProfileId(nutritionistAccount)).thenReturn(Optional.of(nutritionist));
        when(references.hasActiveRelationship(nutritionist, patient)).thenReturn(true);
        when(references.isActiveFood(food)).thenReturn(true);
        when(repository.findByNutritionistAndPatient(nutritionist, patient)).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(invocation -> {
            var value = invocation.<Prescription>getArgument(0);
            stored.set(value);
            return value;
        });
        when(repository.findByVersionId(any())).thenAnswer(invocation -> Optional.of(stored.get()));
        when(repository.findById(any())).thenAnswer(invocation -> Optional.of(stored.get()));
        when(references.patientProfileId(patientAccount)).thenReturn(Optional.of(patient));
        var useCase = new PrescriptionUseCase(repository, references,
            Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC));
        var actor = new Actor(nutritionistAccount, "NUTRITIONIST");

        var created = useCase.create(new Create(actor, patient, content(food)));
        var published = useCase.publish(new VersionQuery(actor, created.versions().getFirst().id()));
        useCase.createVersion(new PrescriptionQuery(actor, created.id()));
        var patientView = useCase.get(new PrescriptionQuery(
            new Actor(patientAccount, "PATIENT"), created.id()));

        assertEquals(PrescriptionVersionStatus.PUBLISHED, published.status());
        assertEquals(1, patientView.versions().size());
        assertEquals(PrescriptionVersionStatus.PUBLISHED, patientView.versions().getFirst().status());
        verify(references, atLeastOnce()).hasActiveRelationship(nutritionist, patient);
    }

    @Test
    void deniesAnUnlinkedNutritionist() {
        var repository = mock(PrescriptionRepository.class);
        var references = mock(PrescriptionReferences.class);
        var account = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        when(references.nutritionistProfileId(account)).thenReturn(Optional.of(nutritionist));
        var useCase = new PrescriptionUseCase(repository, references, Clock.systemUTC());

        assertThrows(PrescriptionAccessDeniedException.class,
            () -> useCase.create(new Create(new Actor(account, "NUTRITIONIST"),
                UUID.randomUUID(), new PrescriptionContentInput(null, null, List.of()))));
        verify(repository, never()).save(any());
    }

    private PrescriptionContentInput content(UUID food) {
        var item = new PrescriptionContentInput.ItemInput(
            food, null, BigDecimal.ONE, "g", 0, null, List.of());
        var meal = new PrescriptionContentInput.MealInput(
            "Breakfast", 0, null, null, List.of(item));
        return new PrescriptionContentInput("Plan", null, List.of(meal));
    }
}

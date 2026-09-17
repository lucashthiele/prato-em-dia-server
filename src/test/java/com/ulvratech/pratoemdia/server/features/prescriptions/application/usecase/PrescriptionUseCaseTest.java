package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionContentInput;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.Actor;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.Create;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PatientQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PrescriptionQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.UpdateVersion;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.VersionQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.InvalidPrescriptionReferenceException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionConflictException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionNotFoundException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(repository.findByNutritionistAndPatient(nutritionist, patient)).thenAnswer(
                invocation -> stored.get() == null ? List.of() : List.of(stored.get())
        );
        when(repository.save(any())).thenAnswer(invocation -> {
            var value = invocation.<Prescription>getArgument(0);
            stored.set(value);
            return value;
        });
        when(repository.findByVersionId(any())).thenAnswer(invocation -> Optional.of(stored.get()));
        when(repository.findById(any())).thenAnswer(invocation -> Optional.of(stored.get()));
        when(references.patientProfileId(patientAccount)).thenReturn(Optional.of(patient));
        var useCase = new PrescriptionUseCase(
                repository, references,
                Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC)
        );
        var actor = new Actor(nutritionistAccount, "NUTRITIONIST");

        var created = useCase.create(new Create(actor, patient, content(food)));
        var published = useCase.publish(new VersionQuery(actor, created.versions().getFirst().id()));
        var draft = useCase.createVersion(new PrescriptionQuery(actor, created.id()));
        var updated = useCase.updateVersion(new UpdateVersion(actor, draft.id(), content(food)));
        var listed = useCase.list(new PatientQuery(actor, patient));
        var versions = useCase.listVersions(new PrescriptionQuery(actor, created.id()));
        var selected = useCase.getVersion(new VersionQuery(actor, draft.id()));
        var patientView = useCase.get(
                new PrescriptionQuery(
                        new Actor(patientAccount, "PATIENT"), created.id()
                )
        );
        when(repository.findCurrentByPatient(patient)).thenReturn(Optional.of(stored.get()));
        var current = useCase.current(new Actor(patientAccount, "PATIENT"));
        var archived = useCase.archive(new VersionQuery(actor, published.id()));

        assertEquals(PrescriptionVersionStatus.PUBLISHED, published.status());
        assertEquals(draft.id(), updated.id());
        assertEquals(draft.id(), selected.id());
        assertEquals(1, listed.size());
        assertEquals(2, versions.size());
        assertEquals(1, patientView.versions().size());
        assertEquals(PrescriptionVersionStatus.PUBLISHED, patientView.versions().getFirst().status());
        assertEquals(published.id(), current.id());
        assertEquals(PrescriptionVersionStatus.ARCHIVED, archived.status());
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

        assertThrows(
                PrescriptionAccessDeniedException.class,
                () -> useCase.create(
                        new Create(
                                new Actor(account, "NUTRITIONIST"),
                                UUID.randomUUID(), new PrescriptionContentInput(null, null, List.of())
                        )
                )
        );
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsInvalidReferencesAndUnauthorizedReaders() {
        var repository = mock(PrescriptionRepository.class);
        var references = mock(PrescriptionReferences.class);
        var account = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var patient = UUID.randomUUID();
        when(references.nutritionistProfileId(account)).thenReturn(Optional.of(nutritionist));
        when(references.hasActiveRelationship(nutritionist, patient)).thenReturn(true);
        var useCase = new PrescriptionUseCase(repository, references, Clock.systemUTC());

        assertThrows(
                InvalidPrescriptionReferenceException.class,
                () -> useCase.create(
                        new Create(
                                new Actor(account, "NUTRITIONIST"), patient,
                                content(UUID.randomUUID())
                        )
                )
        );
        assertThrows(
                PrescriptionNotFoundException.class,
                () -> useCase.get(new PrescriptionQuery(new Actor(account, "ADMIN"), UUID.randomUUID()))
        );
        assertThrows(
                PrescriptionAccessDeniedException.class,
                () -> useCase.current(new Actor(account, "ADMIN"))
        );
    }

    @Test
    void rejectsDuplicatePrescriptionAndMissingProfiles() {
        var repository = mock(PrescriptionRepository.class);
        var references = mock(PrescriptionReferences.class);
        var account = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var patient = UUID.randomUUID();
        when(references.nutritionistProfileId(account)).thenReturn(Optional.of(nutritionist));
        when(references.hasActiveRelationship(nutritionist, patient)).thenReturn(true);
        when(repository.findByNutritionistAndPatient(nutritionist, patient))
                .thenReturn(List.of(mock(Prescription.class)));
        var useCase = new PrescriptionUseCase(repository, references, Clock.systemUTC());

        assertThrows(
                PrescriptionConflictException.class,
                () -> useCase.create(
                        new Create(
                                new Actor(account, "NUTRITIONIST"), patient,
                                new PrescriptionContentInput(null, null, List.of())
                        )
                )
        );
        assertThrows(
                PrescriptionAccessDeniedException.class,
                () -> new PrescriptionUseCase(repository, mock(PrescriptionReferences.class), Clock.systemUTC())
                        .list(new PatientQuery(new Actor(account, "NUTRITIONIST"), patient))
        );
    }

    private PrescriptionContentInput content(UUID food) {
        var item = new PrescriptionContentInput.ItemInput(
                food, null, BigDecimal.ONE, "g", 0, null, List.of()
        );
        var meal = new PrescriptionContentInput.MealInput(
                "Breakfast", 0, null, null, List.of(item)
        );
        return new PrescriptionContentInput("Plan", null, List.of(meal));
    }
}

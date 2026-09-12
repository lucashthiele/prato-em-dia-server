package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RotateInitialPassword;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.*;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.*;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.*;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.*;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RelationshipUseCasesTest {
    private final Instant now = Instant.parse("2026-09-11T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void endsRelationshipAsNutritionistAndPatient() {
        var repository = mock(RelationshipRepository.class);
        var relationship = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        var view = view(relationship);
        when(repository.findActiveViewByNutritionistAccountAndPatient(any(), any()))
            .thenReturn(Optional.of(view));
        when(repository.findActiveViewByPatientAccount(any())).thenReturn(Optional.of(view));
        when(repository.findById(view.relationshipId())).thenReturn(Optional.of(relationship));

        new EndNutritionistRelationshipUseCase(repository, clock).execute(
            new RelationshipQueryInput(UUID.randomUUID(), view.patientId()));
        assertEquals(RelationshipStatus.ENDED, relationship.getStatus());

        var second = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findActiveViewByPatientAccount(any())).thenReturn(Optional.of(view(second)));
        when(repository.findById(second.getId())).thenReturn(Optional.of(second));
        new EndPatientRelationshipUseCase(repository, clock).execute(UUID.randomUUID());
        assertEquals(RelationshipStatus.ENDED, second.getStatus());
    }

    @Test
    void endOperationsReportMissingViewsAndEntities() {
        var repository = mock(RelationshipRepository.class);
        var input = new RelationshipQueryInput(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(RelationshipConflictException.class,
            () -> new EndNutritionistRelationshipUseCase(repository, clock).execute(input));
        assertThrows(RelationshipConflictException.class,
            () -> new EndPatientRelationshipUseCase(repository, clock).execute(UUID.randomUUID()));
        when(repository.findActiveViewByNutritionistAccountAndPatient(any(), any()))
            .thenReturn(Optional.of(view(ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID()))));
        assertThrows(RelationshipNotFoundException.class,
            () -> new EndNutritionistRelationshipUseCase(repository, clock).execute(input));
    }

    @Test
    void getsAndListsRelationshipViews() {
        var repository = mock(RelationshipRepository.class);
        var relationship = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        var view = view(relationship); var account = UUID.randomUUID();
        when(repository.findActiveViewByNutritionistAccountAndPatient(account, view.patientId()))
            .thenReturn(Optional.of(view));
        when(repository.findActiveViewByPatientAccount(account)).thenReturn(Optional.of(view));
        when(repository.findActiveByNutritionistAccount(eq(account), any()))
            .thenReturn(new PageResult<>(List.of(view), 1, 5, 1, 1));

        assertEquals(view.relationshipId(), new GetNutritionistPatientUseCase(repository)
            .execute(new RelationshipQueryInput(account, view.patientId())).relationshipId());
        assertEquals(view.relationshipId(), new GetPatientRelationshipUseCase(repository)
            .execute(account).relationshipId());
        var page = new ListNutritionistPatientsUseCase(repository)
            .execute(new ListRelationshipsInput(account, 1, 5));
        assertEquals(1, page.content().size());
        assertEquals(1, page.totalElements());
    }

    @Test
    void getOperationsReportMissingRelationships() {
        var repository = mock(RelationshipRepository.class);
        assertThrows(RelationshipNotFoundException.class,
            () -> new GetPatientRelationshipUseCase(repository).execute(UUID.randomUUID()));
        assertThrows(RelationshipNotFoundException.class,
            () -> new GetNutritionistPatientUseCase(repository).execute(
                new RelationshipQueryInput(UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void resendRecordsSuccessfulAndFailedDeliveries() {
        var repository = mock(RelationshipRepository.class);
        var rotate = mock(RotateInitialPassword.class);
        var emails = mock(PatientRelationshipEmailSender.class);
        var transactions = transactionTemplate();
        var relationship = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        var view = view(relationship);
        when(repository.findActiveViewByNutritionistAccountAndPatient(any(), any()))
            .thenReturn(Optional.of(view));
        when(repository.findById(view.relationshipId())).thenReturn(Optional.of(relationship));
        when(rotate.execute(view.patientUserAccountId())).thenReturn("temporary password");
        var useCase = new ResendPatientAccessEmailUseCase(repository, rotate, emails, transactions, clock);
        var input = new RelationshipQueryInput(view.nutritionistUserAccountId(), view.patientId());

        useCase.execute(input);
        assertEquals(RelationshipEmailStatus.SENT, relationship.getEmailDeliveryStatus());

        doThrow(new RuntimeException("smtp down")).when(emails)
            .sendInitialAccess(any(), any(), any(), any());
        assertThrows(RelationshipEmailException.class, () -> useCase.execute(input));
        assertEquals(RelationshipEmailStatus.FAILED, relationship.getEmailDeliveryStatus());
        assertEquals(2, relationship.getEmailAttemptCount());
    }

    @Test
    void resendReportsMissingViewAndEntity() {
        var repository = mock(RelationshipRepository.class);
        var useCase = new ResendPatientAccessEmailUseCase(repository, mock(RotateInitialPassword.class),
            mock(PatientRelationshipEmailSender.class), transactionTemplate(), clock);
        var input = new RelationshipQueryInput(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(RelationshipNotFoundException.class, () -> useCase.execute(input));

        var relationship = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findActiveViewByNutritionistAccountAndPatient(any(), any()))
            .thenReturn(Optional.of(view(relationship)));
        assertThrows(RelationshipNotFoundException.class, () -> useCase.execute(input));
    }

    private TransactionTemplate transactionTemplate() {
        var transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(inv ->
            ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));
        doAnswer(inv -> {
            ((java.util.function.Consumer<TransactionStatus>) inv.getArgument(0))
                .accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        return transactions;
    }

    private RelationshipView view(ProfessionalPatientRelationship relationship) {
        return new RelationshipView(relationship.getId(), relationship.getPatientId(), UUID.randomUUID(),
            "Patient", "patient@example.com", relationship.getNutritionistId(), UUID.randomUUID(),
            "Nutritionist", relationship.getStatus(), relationship.getEmailDeliveryStatus(), now);
    }
}

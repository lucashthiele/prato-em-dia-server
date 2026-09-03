package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionPatientAccount;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CreateRelationshipUseCaseTest {
    @Test
    void keepsProvisioningAndReportsFailedEmail() {
        var repository = mock(RelationshipRepository.class);
        var accounts = mock(ProvisionPatientAccount.class);
        var emails = mock(PatientRelationshipEmailSender.class);
        var transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any()))
            .thenAnswer(
                inv ->
                    ((TransactionCallback<?>) inv.getArgument(0))
                        .doInTransaction(mock(TransactionStatus.class)));
        doAnswer(
            inv -> {
                ((TransactionCallbackWithoutResult) inv.getArgument(0))
                    .doInTransaction(mock(TransactionStatus.class));
                return null;
            })
            .when(transactions)
            .executeWithoutResult(any());
        var nAccount = UUID.randomUUID();
        var nProfile = UUID.randomUUID();
        var pAccount = UUID.randomUUID();
        var pProfile = UUID.randomUUID();
        var relationshipId = UUID.randomUUID();
        var now = Instant.parse("2026-09-01T12:00:00Z");
        when(repository.findNutritionistProfileIdByAccount(nAccount))
            .thenReturn(Optional.of(nProfile));
        when(accounts.execute(any()))
            .thenReturn(
                new ProvisionPatientAccountOutput(
                    pAccount,
                    "patient@example.com",
                    true,
                    true,
                    "TemporaryPassword123"));
        when(repository.findPatientProfileIdByAccount(pAccount)).thenReturn(Optional.of(pProfile));
        when(repository.findActiveByPatient(pProfile)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var pending =
            new RelationshipView(
                relationshipId,
                pProfile,
                pAccount,
                "Patient",
                "patient@example.com",
                nProfile,
                nAccount,
                "Nutritionist",
                RelationshipStatus.ACTIVE,
                RelationshipEmailStatus.PENDING,
                now);
        var failed =
            new RelationshipView(
                relationshipId,
                pProfile,
                pAccount,
                "Patient",
                "patient@example.com",
                nProfile,
                nAccount,
                "Nutritionist",
                RelationshipStatus.ACTIVE,
                RelationshipEmailStatus.FAILED,
                now);
        when(repository.findViewById(any())).thenReturn(Optional.of(pending), Optional.of(failed));
        when(repository.findById(relationshipId))
            .thenReturn(
                Optional.of(ProfessionalPatientRelationship.create(nProfile, pProfile)));
        doThrow(new RuntimeException("smtp down"))
            .when(emails)
            .sendInitialAccess(any(), any(), any(), any());
        var result =
            new CreateRelationshipUseCase(
                repository,
                accounts,
                emails,
                transactions,
                Clock.fixed(now, ZoneOffset.UTC))
                .execute(
                    new CreateRelationshipInput(
                        nAccount, "patient@example.com", "Patient"));
        assertEquals("FAILED", result.emailDeliveryStatus());
    }
}

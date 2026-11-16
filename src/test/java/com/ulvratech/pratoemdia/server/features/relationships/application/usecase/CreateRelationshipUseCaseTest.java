package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionPatientAccount;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.service.RelationshipEmailDelivery;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateRelationshipUseCaseTest {
    @Test
    void returnsPendingWhileEmailIsScheduled() {
        var repository = mock(RelationshipRepository.class);
        var accounts = mock(ProvisionPatientAccount.class);
        var emails = mock(RelationshipEmailDelivery.class);
        var transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any()))
                .thenAnswer(
                        inv -> ((TransactionCallback<?>) inv.getArgument(0))
                                .doInTransaction(mock(TransactionStatus.class))
                );
        doAnswer(
                inv -> {
                    ((TransactionCallbackWithoutResult) inv.getArgument(0))
                            .doInTransaction(mock(TransactionStatus.class));
                    return null;
                }
        )
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
                                "TemporaryPassword123"
                        )
                );
        when(repository.findPatientProfileIdByAccount(pAccount)).thenReturn(Optional.of(pProfile));
        when(repository.findActiveByPatient(pProfile)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var pending = new RelationshipView(
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
                now
        );
        when(repository.findViewById(any())).thenReturn(Optional.of(pending));
        var result = new CreateRelationshipUseCase(
                repository,
                accounts,
                emails,
                transactions,
                Clock.fixed(now, ZoneOffset.UTC)
        )
                .execute(
                        new CreateRelationshipInput(
                                nAccount, "patient@example.com", "Patient"
                        )
                );
        assertEquals("PENDING", result.emailDeliveryStatus());
    }

    @Test
    void reportsAlreadyExistsForOwnActiveRelationshipWithoutSendingEmail() {
        var f = fixture();
        when(f.repository.findActiveByPatient(f.pProfile))
                .thenReturn(Optional.of(ProfessionalPatientRelationship.create(f.nProfile, f.pProfile)));
        var e = assertThrows(RelationshipAlreadyExistsException.class, f::run);
        assertEquals(f.pProfile, e.getPatientId());
        assertEquals("RELATIONSHIP_ALREADY_EXISTS", e.getCode());
        verify(f.repository, never()).save(any());
        verifyNoInteractions(f.emails);
    }

    @Test
    void reportsConflictForActiveRelationshipWithAnotherNutritionist() {
        var f = fixture();
        when(f.repository.findActiveByPatient(f.pProfile))
                .thenReturn(Optional.of(ProfessionalPatientRelationship.create(UUID.randomUUID(), f.pProfile)));
        assertThrows(RelationshipConflictException.class, f::run);
        verify(f.repository, never()).save(any());
        verifyNoInteractions(f.emails);
    }

    private Fixture fixture() {
        var repository = mock(RelationshipRepository.class);
        var accounts = mock(ProvisionPatientAccount.class);
        var emails = mock(RelationshipEmailDelivery.class);
        var transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any()))
                .thenAnswer(
                        inv -> ((TransactionCallback<?>) inv.getArgument(0))
                                .doInTransaction(mock(TransactionStatus.class))
                );
        var nAccount = UUID.randomUUID();
        var nProfile = UUID.randomUUID();
        var pAccount = UUID.randomUUID();
        var pProfile = UUID.randomUUID();
        when(repository.findNutritionistProfileIdByAccount(nAccount)).thenReturn(Optional.of(nProfile));
        when(accounts.execute(any()))
                .thenReturn(new ProvisionPatientAccountOutput(pAccount, "patient@example.com", false, false, null));
        when(repository.findPatientProfileIdByAccount(pAccount)).thenReturn(Optional.of(pProfile));
        return new Fixture(repository, accounts, emails, transactions, nAccount, nProfile, pProfile);
    }

    private record Fixture(
            RelationshipRepository repository, ProvisionPatientAccount accounts,
            RelationshipEmailDelivery emails, TransactionTemplate transactions,
            UUID nAccount, UUID nProfile, UUID pProfile) {
        void run() {
            new CreateRelationshipUseCase(
                    repository, accounts, emails, transactions,
                    Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC)
            ).execute(new CreateRelationshipInput(nAccount, "patient@example.com", "Patient"));
        }
    }
}

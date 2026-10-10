package com.ulvratech.pratoemdia.server.features.relationships.application.service;

import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelationshipEmailDeliveryTest {
    @Test
    void recordsCompletionOnlyAfterSendFinishesAndIgnoresOlderAttempt() {
        var sender = mock(PatientRelationshipEmailSender.class);
        var repository = mock(RelationshipRepository.class);
        var transactions = mock(TransactionTemplate.class);
        doAnswer(inv -> {
            ((java.util.function.Consumer<TransactionStatus>) inv.getArgument(0))
                    .accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        var relationship = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        var first = relationship.startEmailAttempt(Instant.now());
        when(repository.findById(relationship.getId())).thenReturn(Optional.of(relationship));
        var pending = new CompletableFuture<Void>();
        when(sender.sendInitialAccess(any(), any(), any(), any())).thenReturn(pending);
        var delivery = new RelationshipEmailDelivery(sender, repository, transactions);

        delivery.sendInitialAccess(
                relationship.getId(), first, "Patient", "patient@example.com", "Nutritionist", "secret"
        );
        assertEquals(RelationshipEmailStatus.PENDING, relationship.getEmailDeliveryStatus());

        var second = relationship.startEmailAttempt(Instant.now());
        pending.complete(null);
        assertEquals(RelationshipEmailStatus.PENDING, relationship.getEmailDeliveryStatus());

        var failed = new CompletableFuture<Void>();
        when(sender.sendInitialAccess(any(), any(), any(), any())).thenReturn(failed);
        delivery.sendInitialAccess(
                relationship.getId(), second, "Patient", "patient@example.com", "Nutritionist", "secret"
        );
        failed.completeExceptionally(new RuntimeException("Resend unavailable"));
        assertEquals(RelationshipEmailStatus.FAILED, relationship.getEmailDeliveryStatus());
        assertEquals(2, relationship.getEmailAttemptCount());
    }
}

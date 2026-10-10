package com.ulvratech.pratoemdia.server.features.relationships.application.service;

import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@Slf4j
public class RelationshipEmailDelivery {
    private final PatientRelationshipEmailSender sender;
    private final RelationshipRepository relationships;
    private final TransactionTemplate transactions;

    public RelationshipEmailDelivery(
            PatientRelationshipEmailSender sender,
            RelationshipRepository relationships,
            TransactionTemplate transactions
    ) {
        this.sender = sender;
        this.relationships = relationships;
        this.transactions = transactions;
    }

    public void sendInitialAccess(
            UUID relationshipId,
            int attempt,
            String name,
            String email,
            String nutritionist,
            String password
    ) {
        dispatch(relationshipId, attempt, () -> sender.sendInitialAccess(name, email, nutritionist, password));
    }

    public void sendRelationshipNotice(
            UUID relationshipId, int attempt, String name, String email, String nutritionist
    ) {
        dispatch(relationshipId, attempt, () -> sender.sendRelationshipNotice(name, email, nutritionist));
    }

    private void dispatch(UUID relationshipId, int attempt, Supplier<CompletableFuture<Void>> send) {
        try {
            send.get().whenComplete(
                    (ignored, error) -> {
                        if (error != null)
                            log.error(
                                    "Failed to send patient relationship email. relationshipId={}", relationshipId,
                                    error
                            );
                        record(
                                relationshipId, attempt,
                                error == null ? RelationshipEmailStatus.SENT : RelationshipEmailStatus.FAILED
                        );
                    }
            );
        } catch (RuntimeException error) {
            log.error("Failed to schedule patient relationship email. relationshipId={}", relationshipId, error);
            record(relationshipId, attempt, RelationshipEmailStatus.FAILED);
        }
    }

    private void record(UUID relationshipId, int attempt, RelationshipEmailStatus result) {
        try {
            transactions.executeWithoutResult(
                    status -> relationships.findById(relationshipId).ifPresent(
                            relationship -> {
                                if (relationship.completeEmailAttempt(attempt, result))
                                    relationships.save(relationship);
                            }
                    )
            );
        } catch (RuntimeException error) {
            log.error("Failed to record patient relationship email status. relationshipId={}", relationshipId, error);
        }
    }
}

package com.ulvratech.pratoemdia.server.features.email.application.usecase;

import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;
import com.ulvratech.pratoemdia.server.features.email.application.exception.EmailDeliveryException;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.DispatchDueEmails;
import com.ulvratech.pratoemdia.server.features.email.application.port.out.EmailSender;
import com.ulvratech.pratoemdia.server.features.email.application.service.AccessEmailTemplate;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InitialPasswordNotAvailableException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RotateInitialPassword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

/**
 * Attempts every due outbox email (one transaction per email). Access emails get a new temporary
 * password inside the attempt's transaction: if sending fails, that transaction rolls back, so a
 * password is only stored when its email was accepted by the provider. The failure itself is then
 * recorded in a separate transaction.
 */
@Service
@Slf4j
public class DispatchDueEmailsUseCase implements DispatchDueEmails {
    static final int BATCH_SIZE = 50;

    private final OutboxEmailRepository repository;
    private final EmailSender sender;
    private final RotateInitialPassword rotateInitialPassword;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final String appUrl;

    public DispatchDueEmailsUseCase(
            OutboxEmailRepository repository,
            EmailSender sender,
            RotateInitialPassword rotateInitialPassword,
            TransactionTemplate transactions,
            Clock clock,
            @Value("${relationships.mail.app-url}") String appUrl
    ) {
        this.repository = repository;
        this.sender = sender;
        this.rotateInitialPassword = rotateInitialPassword;
        this.transactions = transactions;
        this.clock = clock;
        this.appUrl = appUrl;
    }

    @Override
    public void execute() {
        for (UUID id : repository.findDueIds(clock.instant(), BATCH_SIZE)) {
            try {
                attempt(id);
            } catch (RuntimeException error) {
                log.error("Unexpected failure while dispatching outbox email. emailId={}", id, error);
            }
        }
    }

    private void attempt(UUID id) {
        try {
            transactions.executeWithoutResult(status -> deliver(id));
        } catch (EmailDeliveryException failure) {
            transactions.executeWithoutResult(status -> recordFailure(id, failure));
        } catch (InitialPasswordNotAvailableException alreadyChanged) {
            // Thrown through a transactional proxy, so the attempt's transaction is already rolled back.
            transactions.executeWithoutResult(
                    status -> recordFailure(id, new EmailDeliveryException("PASSWORD_ALREADY_CHANGED", false))
            );
        }
    }

    private void deliver(UUID id) {
        var email = repository.findByIdForUpdate(id).orElse(null);
        if (email == null || email.getStatus() != EmailStatus.PENDING) {
            return;
        }
        var now = clock.instant();
        OutgoingEmail outgoing;
        if (email.getType() == EmailType.ACCESS) {
            var password = rotateInitialPassword.execute(email.getAccountId());
            outgoing = AccessEmailTemplate.build(email, password, appUrl);
        } else {
            outgoing = new OutgoingEmail(email.getRecipient(), email.getSubject(), email.getBody());
        }
        sender.send(outgoing);
        email.markSent(now);
        repository.save(email);
    }

    private void recordFailure(UUID id, EmailDeliveryException failure) {
        repository.findByIdForUpdate(id).filter(e -> e.getStatus() == EmailStatus.PENDING).ifPresent(email -> {
            if (failure.isTransientFailure()) {
                email.recordTransientFailure(clock.instant(), failure.getMessage());
            } else {
                email.recordPermanentFailure(clock.instant(), failure.getMessage());
            }
            repository.save(email);
            if (email.getStatus() == EmailStatus.DEAD) {
                log.error(
                        "Outbox email is DEAD after {} attempts. emailId={}, type={}", email.getAttempts(), id,
                        email.getType()
                );
            }
        });
    }
}

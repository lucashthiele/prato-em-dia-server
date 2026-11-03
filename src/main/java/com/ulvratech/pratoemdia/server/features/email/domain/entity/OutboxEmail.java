package com.ulvratech.pratoemdia.server.features.email.domain.entity;

import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Email waiting to be sent. Plan emails keep their subject and body; access emails keep only the
 * account reference, because the temporary password is generated on every attempt.
 */
public class OutboxEmail {
    public static final int MAX_ATTEMPTS = 3;
    public static final Duration RETRY_DELAY = Duration.ofMinutes(10);
    private static final int MAX_ERROR_LENGTH = 500;

    private final UUID id;
    private final EmailType type;
    private final String recipient;
    private final String recipientName;
    private final String inviterName;
    private final UUID accountId;
    private final String subject;
    private final String body;
    private final Instant createdAt;
    private EmailStatus status;
    private int attempts;
    private Instant nextAttemptAt;
    private String lastError;
    private Instant sentAt;

    public OutboxEmail(
            UUID id, EmailType type, String recipient, String recipientName, String inviterName, UUID accountId,
            String subject, String body, EmailStatus status, int attempts, Instant nextAttemptAt, String lastError,
            Instant createdAt, Instant sentAt
    ) {
        this.id = id;
        this.type = type;
        this.recipient = recipient;
        this.recipientName = recipientName;
        this.inviterName = inviterName;
        this.accountId = accountId;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static OutboxEmail plan(EmailType type, String recipient, String subject, String body, Instant now) {
        if (type == EmailType.ACCESS) {
            throw new IllegalArgumentException("Access emails never store their body");
        }
        return new OutboxEmail(
                UUID.randomUUID(), type, recipient, null, null, null, subject, body, EmailStatus.PENDING, 0, now,
                null, now, null
        );
    }

    public static OutboxEmail access(
            UUID accountId, String recipient, String recipientName, String inviterName, Instant now
    ) {
        return new OutboxEmail(
                UUID.randomUUID(), EmailType.ACCESS, recipient, recipientName, inviterName, accountId, null, null,
                EmailStatus.PENDING, 0, now, null, now, null
        );
    }

    public void markSent(Instant now) {
        requirePending();
        attempts++;
        status = EmailStatus.SENT;
        sentAt = now;
        lastError = null;
    }

    /** Server-side failure (timeout, 5xx, 429): retry in ten minutes, until the attempts run out. */
    public void recordTransientFailure(Instant now, String error) {
        requirePending();
        attempts++;
        lastError = truncate(error);
        if (attempts >= MAX_ATTEMPTS) {
            status = EmailStatus.DEAD;
        } else {
            nextAttemptAt = now.plus(RETRY_DELAY);
        }
    }

    /** Failure that retrying cannot fix (provider 4xx, account no longer eligible). */
    public void recordPermanentFailure(Instant now, String error) {
        requirePending();
        attempts++;
        status = EmailStatus.FAILED;
        lastError = truncate(error);
    }

    private void requirePending() {
        if (status != EmailStatus.PENDING) {
            throw new IllegalStateException("Email already finished with status " + status);
        }
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    public UUID getId() {
        return id;
    }

    public EmailType getType() {
        return type;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getInviterName() {
        return inviterName;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}

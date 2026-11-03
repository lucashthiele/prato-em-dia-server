package com.ulvratech.pratoemdia.server.features.email.domain.entity;

import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxEmailTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

    private OutboxEmail plan() {
        return OutboxEmail.plan(EmailType.PLAN_GENERATED, "maria@example.com", "Assunto", "Corpo", NOW);
    }

    @Test
    void newPlanEmailIsPendingNowAndKeepsItsBody() {
        var email = plan();

        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(0, email.getAttempts());
        assertEquals(NOW, email.getNextAttemptAt());
        assertEquals("Corpo", email.getBody());
        assertNull(email.getAccountId());
    }

    @Test
    void newAccessEmailKeepsOnlyAReferenceAndNeverABody() {
        var account = UUID.randomUUID();

        var email = OutboxEmail.access(account, "nutri@example.com", "Nutri", "Admin", NOW);

        assertEquals(EmailType.ACCESS, email.getType());
        assertEquals(account, email.getAccountId());
        assertNull(email.getBody());
        assertNull(email.getSubject());
        assertEquals(EmailStatus.PENDING, email.getStatus());
    }

    @Test
    void successMarksSentAndCountsTheAttempt() {
        var email = plan();

        email.markSent(NOW);

        assertEquals(EmailStatus.SENT, email.getStatus());
        assertEquals(1, email.getAttempts());
        assertEquals(NOW, email.getSentAt());
    }

    @Test
    void transientFailureReschedulesTenMinutesLaterUntilThreeAttempts() {
        var email = plan();

        email.recordTransientFailure(NOW, "timeout");
        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(1, email.getAttempts());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), email.getNextAttemptAt());
        assertEquals("timeout", email.getLastError());

        email.recordTransientFailure(NOW.plusSeconds(600), "503");
        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(2, email.getAttempts());
        assertEquals(NOW.plusSeconds(1200), email.getNextAttemptAt());

        email.recordTransientFailure(NOW.plusSeconds(1200), "503");
        assertEquals(EmailStatus.DEAD, email.getStatus());
        assertEquals(3, email.getAttempts());
    }

    @Test
    void permanentFailureGoesStraightToFailed() {
        var email = plan();

        email.recordPermanentFailure(NOW, "422 invalid recipient");

        assertEquals(EmailStatus.FAILED, email.getStatus());
        assertEquals(1, email.getAttempts());
        assertEquals("422 invalid recipient", email.getLastError());
    }

    @Test
    void finishedEmailsCannotBeAttemptedAgain() {
        var email = plan();
        email.markSent(NOW);

        assertThrows(IllegalStateException.class, () -> email.markSent(NOW));
        assertThrows(IllegalStateException.class, () -> email.recordTransientFailure(NOW, "x"));
        assertTrue(email.getStatus().isFinal());
    }
}

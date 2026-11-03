package com.ulvratech.pratoemdia.server.features.email.infrastructure.sender;

import com.resend.core.exception.ResendException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendEmailSenderTest {
    @Test
    void timeoutsServerErrorsAndRateLimitsAreRetried() {
        assertTrue(ResendEmailSender.isTransient(new ResendException("timeout", new java.io.IOException("timed out"))));
        assertTrue(ResendEmailSender.isTransient(new ResendException(500, "boom")));
        assertTrue(ResendEmailSender.isTransient(new ResendException(503, "unavailable")));
        assertTrue(ResendEmailSender.isTransient(new ResendException(429, "slow down")));
    }

    @Test
    void otherClientErrorsAreFinal() {
        assertFalse(ResendEmailSender.isTransient(new ResendException(400, "bad")));
        assertFalse(ResendEmailSender.isTransient(new ResendException(401, "key")));
        assertFalse(ResendEmailSender.isTransient(new ResendException(403, "domain")));
        assertFalse(ResendEmailSender.isTransient(new ResendException(422, "invalid recipient")));
    }
}

package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.TooManyLoginAttemptsException;
import com.ulvratech.pratoemdia.server.features.identity.config.LoginRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryLoginAttemptLimiterTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-03T12:00:00Z"));
    private final InMemoryLoginAttemptLimiter limiter = new InMemoryLoginAttemptLimiter(
            new LoginRateLimitProperties(true, 3, 5, Duration.ofMinutes(15)), clock
    );

    private void fail(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email, ip);
        }
    }

    @Test
    void blocksAnEmailAfterTooManyFailuresAndSaysHowLongToWait() {
        fail("a@example.com", "1.1.1.1", 2);
        assertDoesNotThrow(() -> limiter.checkAllowed("a@example.com", "1.1.1.1"));

        fail("a@example.com", "1.1.1.1", 1);

        var blocked = assertThrows(
                TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("a@example.com", "2.2.2.2")
        );
        assertEquals(15 * 60, blocked.getRetryAfterSeconds());
        assertEquals("LOGIN_RATE_LIMITED", blocked.getCode());
    }

    @Test
    void retryAfterShrinksAsTheOldestFailureAgesOut() {
        fail("a@example.com", "1.1.1.1", 3);
        clock.advance(Duration.ofMinutes(10));

        var blocked = assertThrows(
                TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("a@example.com", "1.1.1.1")
        );
        assertEquals(5 * 60, blocked.getRetryAfterSeconds());
    }

    @Test
    void failuresOlderThanTheWindowStopCounting() {
        fail("a@example.com", "1.1.1.1", 3);
        clock.advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> limiter.checkAllowed("a@example.com", "1.1.1.1"));
    }

    @Test
    void emailsAreNormalizedAndIndependent() {
        fail("A@Example.com ", "1.1.1.1", 3);

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("a@example.com", "9.9.9.9"));
        assertDoesNotThrow(() -> limiter.checkAllowed("other@example.com", "9.9.9.9"));
    }

    @Test
    void anIpIsBlockedAcrossDifferentEmails() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("user" + i + "@example.com", "3.3.3.3");
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("new@example.com", "3.3.3.3"));
        assertDoesNotThrow(() -> limiter.checkAllowed("new@example.com", "4.4.4.4"));
    }

    @Test
    void aSuccessfulLoginClearsTheEmailCounterButNotTheIpCounter() {
        fail("a@example.com", "1.1.1.1", 2);
        limiter.recordSuccess("a@example.com");
        fail("a@example.com", "1.1.1.1", 2);
        fail("c@example.com", "1.1.1.1", 1);

        assertDoesNotThrow(() -> limiter.checkAllowed("a@example.com", "5.5.5.5"));
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("b@example.com", "1.1.1.1"));
    }

    @Test
    void unknownIpIsOnlyLimitedByEmail() {
        fail("a@example.com", null, 2);
        assertDoesNotThrow(() -> limiter.checkAllowed("a@example.com", null));
        fail("a@example.com", null, 1);
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("a@example.com", null));
    }

    @Test
    void disabledLimiterNeverBlocks() {
        var off = new InMemoryLoginAttemptLimiter(
                new LoginRateLimitProperties(false, 1, 1, Duration.ofMinutes(15)), clock
        );
        off.recordFailure("a@example.com", "1.1.1.1");
        off.recordFailure("a@example.com", "1.1.1.1");

        assertDoesNotThrow(() -> off.checkAllowed("a@example.com", "1.1.1.1"));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

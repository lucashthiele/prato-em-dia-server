package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.TooManyLoginAttemptsException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.LoginAttemptLimiter;
import com.ulvratech.pratoemdia.server.features.identity.config.LoginRateLimitProperties;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window counter of failed logins kept in memory. It is per instance and forgotten on
 * restart; see docs/limitacao-de-tentativas-de-login.md for why that is enough for the pilot.
 */
public class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {
    private static final int PRUNE_THRESHOLD = 10_000;

    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public InMemoryLoginAttemptLimiter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void checkAllowed(String email, String ip) {
        if (!properties.enabled()) {
            return;
        }
        var now = clock.instant();
        long wait = Math.max(
                retryAfter(emailKey(email), properties.maxAttemptsPerEmail(), now),
                ip == null ? 0 : retryAfter(ipKey(ip), properties.maxAttemptsPerIp(), now)
        );
        if (wait > 0) {
            throw new TooManyLoginAttemptsException(wait);
        }
    }

    @Override
    public void recordFailure(String email, String ip) {
        if (!properties.enabled()) {
            return;
        }
        var now = clock.instant();
        if (failures.size() > PRUNE_THRESHOLD) {
            prune(now);
        }
        add(emailKey(email), now);
        if (ip != null) {
            add(ipKey(ip), now);
        }
    }

    @Override
    public void recordSuccess(String email) {
        failures.remove(emailKey(email));
    }

    private long retryAfter(String key, int max, Instant now) {
        var window = failures.get(key);
        if (window == null) {
            return 0;
        }
        synchronized (window) {
            trim(window, now);
            if (window.size() < max) {
                return 0;
            }
            // The (size - max + 1)-th oldest failure must leave the window before a new attempt is allowed.
            var blocking = window.stream().skip(window.size() - max).findFirst().orElseThrow();
            return Math.max(1, blocking.plus(properties.window()).getEpochSecond() - now.getEpochSecond());
        }
    }

    private void add(String key, Instant now) {
        var window = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            trim(window, now);
            window.addLast(now);
        }
    }

    private void trim(Deque<Instant> window, Instant now) {
        var limit = now.minus(properties.window());
        while (!window.isEmpty() && !window.peekFirst().isAfter(limit)) {
            window.pollFirst();
        }
    }

    private void prune(Instant now) {
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                trim(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private static String emailKey(String email) {
        return "email:" + email.strip().toLowerCase(Locale.ROOT);
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }
}

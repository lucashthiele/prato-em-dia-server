package com.ulvratech.pratoemdia.server.features.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Failed-login limits (LUC-73). Each key (e-mail, IP) may fail {@code maxAttemptsPer*} times inside
 * a sliding {@code window}; then logins for it get 429 until the oldest failure leaves the window.
 */
@ConfigurationProperties(prefix = "security.login-rate-limit")
public record LoginRateLimitProperties(
        boolean enabled, int maxAttemptsPerEmail, int maxAttemptsPerIp, Duration window) {
    public LoginRateLimitProperties {
        if (maxAttemptsPerEmail < 1 || maxAttemptsPerIp < 1) {
            throw new IllegalArgumentException("security.login-rate-limit max attempts must be at least 1");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("security.login-rate-limit.window must be positive");
        }
    }
}

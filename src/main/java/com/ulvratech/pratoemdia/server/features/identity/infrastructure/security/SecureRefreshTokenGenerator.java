package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedRefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenGenerator;
import com.ulvratech.pratoemdia.server.features.identity.config.JwtConfigurationProperties;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

public class SecureRefreshTokenGenerator implements RefreshTokenGenerator {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;
    private final Clock clock;
    private final JwtConfigurationProperties properties;

    public SecureRefreshTokenGenerator(
        SecureRandom secureRandom,
        Clock clock,
        JwtConfigurationProperties properties
    ) {
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    public GeneratedRefreshToken generate() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        var value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return new GeneratedRefreshToken(
            value,
            clock.instant().plus(properties.refreshTokenTtl())
        );
    }
}

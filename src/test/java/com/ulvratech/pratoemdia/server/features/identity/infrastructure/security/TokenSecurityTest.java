package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.config.JwtConfigurationProperties;
import com.ulvratech.pratoemdia.server.features.identity.config.SecurityConfiguration;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSecurityTest {
    private static final Instant NOW = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

    @Test
    void createsAndValidatesACompactRoleBearingJwt() {
        var properties = properties();
        var configuration = new SecurityConfiguration();
        var key = configuration.jwtSecretKey(properties);
        var encoder = configuration.jwtEncoder(key);
        var decoder = configuration.jwtDecoder(key, properties);
        var provider = new JwtAccessTokenProvider(
                encoder,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        var userId = UUID.randomUUID();
        var user = new UserAccount(
                userId,
                "nutritionist@example.com",
                "password-hash",
                UserAccountRole.NUTRITIONIST,
                UserAccountStatus.ACTIVE
        );

        var generated = provider.generate(user);
        var decoded = decoder.decode(generated.value());

        assertEquals(userId.toString(), decoded.getSubject());
        assertEquals("NUTRITIONIST", decoded.getClaimAsString("role"));
        assertEquals("prato-em-dia", decoded.getClaimAsString("iss"));
        assertEquals(NOW.plusSeconds(900), generated.expiresAt());
        var authentication = new JwtAuthenticationConverter().convert(decoded);
        assertTrue(
                authentication.getAuthorities().stream().anyMatch(
                        authority -> authority.getAuthority().equals("ROLE_NUTRITIONIST")
                )
        );
    }

    @Test
    void refreshTokensAreRandomAndOnlyTheirStableHashesNeedPersistence() {
        var generator = new SecureRefreshTokenGenerator(
                new SecureRandom(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                properties()
        );
        var hasher = new Sha256RefreshTokenHasher();

        var first = generator.generate();
        var second = generator.generate();

        assertNotEquals(first.value(), second.value());
        assertEquals(64, hasher.hash(first.value()).length());
        assertEquals(hasher.hash(first.value()), hasher.hash(first.value()));
        assertEquals(NOW.plus(Duration.ofDays(30)), first.expiresAt());
    }

    @Test
    void rejectsSecretsThatAreTooShortForHs256() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtConfigurationProperties(
                        "prato-em-dia",
                        Duration.ofMinutes(15),
                        Duration.ofDays(30),
                        "too-short"
                )
        );
    }

    private JwtConfigurationProperties properties() {
        return new JwtConfigurationProperties(
                "prato-em-dia",
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                "0123456789abcdef0123456789abcdef"
        );
    }
}

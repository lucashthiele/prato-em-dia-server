package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedRefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.ExpiredRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.AccessTokenProvider;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshAccessTokenUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-08-18T12:00:00Z");

    private RefreshTokenRepository refreshTokens;
    private UserAccountRepository userAccounts;
    private RefreshTokenHasher hasher;
    private RefreshTokenGenerator generator;
    private AccessTokenProvider accessTokens;
    private RefreshAccessTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        refreshTokens = mock(RefreshTokenRepository.class);
        userAccounts = mock(UserAccountRepository.class);
        hasher = mock(RefreshTokenHasher.class);
        generator = mock(RefreshTokenGenerator.class);
        accessTokens = mock(AccessTokenProvider.class);
        useCase = new RefreshAccessTokenUseCase(
                refreshTokens,
                userAccounts,
                hasher,
                generator,
                accessTokens,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void rotatesTheRefreshTokenAndRevokesThePreviousToken() {
        var user = activeUser();
        var current = RefreshToken.issue(
                UUID.randomUUID(),
                user.getId(),
                "old-hash",
                NOW.plusSeconds(3600),
                NOW.minusSeconds(60)
        );
        when(hasher.hash("old-raw-token")).thenReturn("old-hash");
        when(refreshTokens.findByTokenHashForUpdate("old-hash"))
                .thenReturn(Optional.of(current));
        when(userAccounts.findById(user.getId())).thenReturn(Optional.of(user));
        when(generator.generate())
                .thenReturn(new GeneratedRefreshToken("new-raw-token", NOW.plusSeconds(7200)));
        when(hasher.hash("new-raw-token")).thenReturn("new-hash");
        when(accessTokens.generate(user))
                .thenReturn(new GeneratedAccessToken("new-access-token", NOW.plusSeconds(900)));
        when(refreshTokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var output = useCase.execute(new RefreshAccessTokenInput("old-raw-token"));

        assertEquals("new-access-token", output.accessToken());
        assertEquals("new-raw-token", output.refreshToken());
        assertTrue(current.isRevoked());
        verify(refreshTokens)
                .save(org.mockito.ArgumentMatchers.argThat(token -> token.getTokenHash().equals("new-hash")));
        assertTrue(current.getReplacedByTokenId() != null);
    }

    @Test
    void rejectsAnExpiredRefreshToken() {
        var expired = RefreshToken.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "expired-hash",
                NOW,
                NOW.minusSeconds(3600)
        );
        when(hasher.hash("expired-token")).thenReturn("expired-hash");
        when(refreshTokens.findByTokenHashForUpdate("expired-hash"))
                .thenReturn(Optional.of(expired));

        assertThrows(
                ExpiredRefreshTokenException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("expired-token"))
        );
        verify(generator, never()).generate();
    }

    @Test
    void rejectsARevokedRefreshToken() {
        var revoked = RefreshToken.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "revoked-hash",
                NOW.plusSeconds(3600),
                NOW.minusSeconds(60)
        );
        revoked.revoke(NOW.minusSeconds(10), null);
        when(hasher.hash("revoked-token")).thenReturn("revoked-hash");
        when(refreshTokens.findByTokenHashForUpdate("revoked-hash"))
                .thenReturn(Optional.of(revoked));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("revoked-token"))
        );
    }

    private UserAccount activeUser() {
        return new UserAccount(
                UUID.randomUUID(),
                "patient@example.com",
                "password-hash",
                UserAccountRole.PATIENT,
                UserAccountStatus.ACTIVE
        );
    }
}

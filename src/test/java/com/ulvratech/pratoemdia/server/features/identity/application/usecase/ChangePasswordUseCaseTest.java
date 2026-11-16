package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangePasswordInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedRefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.AccessTokenProvider;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangePasswordUseCaseTest {
    @Test
    void replacesTemporaryPasswordRevokesSessionsAndIssuesUnrestrictedTokens() {
        var accounts = mock(UserAccountRepository.class);
        var refreshTokens = mock(RefreshTokenRepository.class);
        var hasher = mock(PasswordHasher.class);
        var accessTokens = mock(AccessTokenProvider.class);
        var generator = mock(RefreshTokenGenerator.class);
        var tokenHasher = mock(RefreshTokenHasher.class);
        var now = Instant.parse("2026-09-01T12:00:00Z");
        var id = UUID.randomUUID();
        var account = new UserAccount(
                id,
                "patient@example.com",
                "old-hash",
                UserAccountRole.PATIENT,
                UserAccountStatus.ACTIVE,
                true
        );
        when(accounts.findById(id)).thenReturn(Optional.of(account));
        when(hasher.matches("TemporaryPassword123", "old-hash")).thenReturn(true);
        when(hasher.hash("PermanentPassword123")).thenReturn("new-hash");
        when(generator.generate())
                .thenReturn(new GeneratedRefreshToken("refresh", now.plus(Duration.ofDays(30))));
        when(tokenHasher.hash("refresh")).thenReturn("refresh-hash");
        when(accessTokens.generate(any()))
                .thenReturn(new GeneratedAccessToken("access", now.plusSeconds(900)));
        var output = new ChangePasswordUseCase(
                accounts,
                refreshTokens,
                hasher,
                accessTokens,
                generator,
                tokenHasher,
                Clock.fixed(now, ZoneOffset.UTC)
        )
                .execute(
                        new ChangePasswordInput(
                                id, "TemporaryPassword123", "PermanentPassword123"
                        )
                );
        assertFalse(output.passwordChangeRequired());
        assertFalse(account.isPasswordChangeRequired());
        assertEquals("new-hash", account.getPasswordHash());
        verify(refreshTokens).revokeAllByUserAccountId(id, now);
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    private ChangePasswordUseCase useCaseFor(
            UserAccount account, PasswordHasher hasher, RefreshTokenRepository refreshTokens
    ) {
        var accounts = mock(UserAccountRepository.class);
        var accessTokens = mock(AccessTokenProvider.class);
        var generator = mock(RefreshTokenGenerator.class);
        var tokenHasher = mock(RefreshTokenHasher.class);
        var now = Instant.parse("2026-09-01T12:00:00Z");
        when(accounts.findById(account.getId())).thenReturn(Optional.of(account));
        when(hasher.hash("PermanentPassword123")).thenReturn("new-hash");
        when(generator.generate()).thenReturn(new GeneratedRefreshToken("refresh", now.plus(Duration.ofDays(30))));
        when(tokenHasher.hash("refresh")).thenReturn("refresh-hash");
        when(accessTokens.generate(any())).thenReturn(new GeneratedAccessToken("access", now.plusSeconds(900)));
        return new ChangePasswordUseCase(
                accounts, refreshTokens, hasher, accessTokens, generator, tokenHasher, Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    private UserAccount account(boolean passwordChangeRequired) {
        return new UserAccount(
                UUID.randomUUID(), "u@example.com", "old-hash", UserAccountRole.PATIENT,
                UserAccountStatus.ACTIVE, passwordChangeRequired
        );
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = { "anything" })
    void ignoresTheCurrentPasswordOnFirstAccess(String currentPassword) {
        var hasher = mock(PasswordHasher.class);
        var account = account(true);

        var output = useCaseFor(account, hasher, mock(RefreshTokenRepository.class))
                .execute(new ChangePasswordInput(account.getId(), currentPassword, "PermanentPassword123"));

        assertFalse(output.passwordChangeRequired());
        assertEquals("new-hash", account.getPasswordHash());
        org.mockito.Mockito.verify(hasher, org.mockito.Mockito.never()).matches(any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = { "wrong" })
    void requiresAMatchingCurrentPasswordAfterFirstAccess(String currentPassword) {
        var hasher = mock(PasswordHasher.class);
        var refreshTokens = mock(RefreshTokenRepository.class);
        var account = account(false);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException.class,
                () -> useCaseFor(account, hasher, refreshTokens)
                        .execute(new ChangePasswordInput(account.getId(), currentPassword, "PermanentPassword123"))
        );
        assertEquals("old-hash", account.getPasswordHash());
        org.mockito.Mockito.verifyNoInteractions(refreshTokens);
    }

    @Test
    void acceptsTheRightCurrentPasswordAfterFirstAccess() {
        var hasher = mock(PasswordHasher.class);
        var account = account(false);
        when(hasher.matches("right", "old-hash")).thenReturn(true);

        useCaseFor(account, hasher, mock(RefreshTokenRepository.class))
                .execute(new ChangePasswordInput(account.getId(), "right", "PermanentPassword123"));

        assertEquals("new-hash", account.getPasswordHash());
    }
}

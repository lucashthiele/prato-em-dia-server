package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedRefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InactiveUserAccountException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticateUserUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-08-18T12:00:00Z");

    private UserAccountRepository userAccounts;
    private RefreshTokenRepository refreshTokens;
    private PasswordHasher passwordHasher;
    private AccessTokenProvider accessTokenProvider;
    private RefreshTokenGenerator refreshTokenGenerator;
    private RefreshTokenHasher refreshTokenHasher;
    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userAccounts = mock(UserAccountRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        accessTokenProvider = mock(AccessTokenProvider.class);
        refreshTokenGenerator = mock(RefreshTokenGenerator.class);
        refreshTokenHasher = mock(RefreshTokenHasher.class);
        useCase = new AuthenticateUserUseCase(
            userAccounts,
            refreshTokens,
            passwordHasher,
            accessTokenProvider,
            refreshTokenGenerator,
            refreshTokenHasher,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void authenticatesAndStoresOnlyTheRefreshTokenHash() {
        var user = user(UserAccountStatus.ACTIVE);
        when(userAccounts.findByEmailIgnoreCase("patient@example.com"))
            .thenReturn(Optional.of(user));
        when(passwordHasher.matches("correct-password", "password-hash"))
            .thenReturn(true);
        when(accessTokenProvider.generate(user))
            .thenReturn(new GeneratedAccessToken("access-token", NOW.plusSeconds(900)));
        when(refreshTokenGenerator.generate())
            .thenReturn(new GeneratedRefreshToken("raw-refresh-token", NOW.plusSeconds(3600)));
        when(refreshTokenHasher.hash("raw-refresh-token")).thenReturn("refresh-token-hash");
        when(refreshTokens.save(any(RefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var output = useCase.execute(new AuthenticateUserInput(
            " Patient@Example.com ",
            "correct-password"
        ));

        assertEquals("access-token", output.accessToken());
        assertEquals("raw-refresh-token", output.refreshToken());
        assertEquals("Bearer", output.tokenType());
        assertEquals(900, output.expiresIn());
        verify(refreshTokens).save(org.mockito.ArgumentMatchers.argThat(token ->
            token.getTokenHash().equals("refresh-token-hash")
                && !token.getTokenHash().equals("raw-refresh-token")
        ));
    }

    @Test
    void rejectsAnIncorrectPasswordWithoutIssuingTokens() {
        var user = user(UserAccountStatus.ACTIVE);
        when(userAccounts.findByEmailIgnoreCase("patient@example.com"))
            .thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", "password-hash"))
            .thenReturn(false);

        assertThrows(
            InvalidCredentialsException.class,
            () -> useCase.execute(new AuthenticateUserInput(
                "patient@example.com",
                "wrong-password"
            ))
        );
        verify(accessTokenProvider, never()).generate(any());
    }

    @Test
    void rejectsAnInactiveAccount() {
        var user = user(UserAccountStatus.INACTIVE);
        when(userAccounts.findByEmailIgnoreCase("patient@example.com"))
            .thenReturn(Optional.of(user));
        when(passwordHasher.matches("correct-password", "password-hash"))
            .thenReturn(true);

        assertThrows(
            InactiveUserAccountException.class,
            () -> useCase.execute(new AuthenticateUserInput(
                "patient@example.com",
                "correct-password"
            ))
        );
        verify(accessTokenProvider, never()).generate(any());
    }

    private UserAccount user(UserAccountStatus status) {
        return new UserAccount(
            UUID.randomUUID(),
            "patient@example.com",
            "password-hash",
            UserAccountRole.PATIENT,
            status
        );
    }
}

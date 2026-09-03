package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.LogoutInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class LogoutUserUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-08-18T12:00:00Z");

    @Test
    void revokesTheAuthenticatedUsersRefreshToken() {
        var repository = mock(RefreshTokenRepository.class);
        var hasher = mock(RefreshTokenHasher.class);
        var userId = UUID.randomUUID();
        var token = RefreshToken.issue(
            UUID.randomUUID(),
            userId,
            "token-hash",
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60)
        );
        when(hasher.hash("raw-token")).thenReturn("token-hash");
        when(repository.findByTokenHashForUpdate("token-hash"))
            .thenReturn(Optional.of(token));
        when(repository.save(token)).thenReturn(token);
        var useCase = new LogoutUserUseCase(
            repository,
            hasher,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        useCase.execute(new LogoutInput(userId, "raw-token"));

        assertTrue(token.isRevoked());
        verify(repository).save(token);
    }

    @Test
    void refusesToRevokeAnotherUsersRefreshToken() {
        var repository = mock(RefreshTokenRepository.class);
        var hasher = mock(RefreshTokenHasher.class);
        var token = RefreshToken.issue(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "token-hash",
            NOW.plusSeconds(3600),
            NOW.minusSeconds(60)
        );
        when(hasher.hash("raw-token")).thenReturn("token-hash");
        when(repository.findByTokenHashForUpdate("token-hash"))
            .thenReturn(Optional.of(token));
        var useCase = new LogoutUserUseCase(
            repository,
            hasher,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThrows(
            InvalidRefreshTokenException.class,
            () -> useCase.execute(new LogoutInput(UUID.randomUUID(), "raw-token"))
        );
    }
}

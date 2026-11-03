package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangeUserAccountStatusInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountNotFoundException;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
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

class ChangeUserAccountStatusUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final ChangeUserAccountStatusUseCase useCase = new ChangeUserAccountStatusUseCase(
            accounts, tokens, Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final UUID id = UUID.randomUUID();

    private UserAccount account(UserAccountStatus status) {
        var account = new UserAccount(id, "a@example.com", "h", UserAccountRole.NUTRITIONIST, status);
        when(accounts.findById(id)).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void deactivatingRevokesEverySessionAndReportsThePreviousStatus() {
        var account = account(UserAccountStatus.ACTIVE);

        var previous = useCase.execute(new ChangeUserAccountStatusInput(id, UserAccountStatus.INACTIVE));

        assertEquals(UserAccountStatus.ACTIVE, previous);
        assertEquals(UserAccountStatus.INACTIVE, account.getStatus());
        verify(accounts).save(account);
        verify(tokens).revokeAllByUserAccountId(id, NOW);
    }

    @Test
    void reactivatingDoesNotTouchTokens() {
        var account = account(UserAccountStatus.INACTIVE);

        useCase.execute(new ChangeUserAccountStatusInput(id, UserAccountStatus.ACTIVE));

        assertEquals(UserAccountStatus.ACTIVE, account.getStatus());
        verify(tokens, never()).revokeAllByUserAccountId(any(), any());
    }

    @Test
    void unknownAccountIsNotFound() {
        when(accounts.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                UserAccountNotFoundException.class,
                () -> useCase.execute(new ChangeUserAccountStatusInput(id, UserAccountStatus.INACTIVE))
        );
    }
}

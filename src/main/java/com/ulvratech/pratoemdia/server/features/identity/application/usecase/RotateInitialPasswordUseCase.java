package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.InitialPasswordNotAvailableException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RotateInitialPassword;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.InitialPasswordGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class RotateInitialPasswordUseCase implements RotateInitialPassword {
    private final UserAccountRepository accounts;
    private final RefreshTokenRepository tokens;
    private final PasswordHasher hasher;
    private final InitialPasswordGenerator generator;
    private final Clock clock;

    public RotateInitialPasswordUseCase(
            UserAccountRepository accounts,
            RefreshTokenRepository tokens,
            PasswordHasher hasher,
            InitialPasswordGenerator generator,
            Clock clock
    ) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.hasher = hasher;
        this.generator = generator;
        this.clock = clock;
    }

    public String execute(UUID id) {
        var account = accounts.findById(id).orElseThrow(InitialPasswordNotAvailableException::new);
        if (!account.isPasswordChangeRequired())
            throw new InitialPasswordNotAvailableException();
        var raw = generator.generate();
        account.replacePassword(hasher.hash(raw), true);
        accounts.save(account);
        tokens.revokeAllByUserAccountId(id, clock.instant());
        return raw;
    }
}

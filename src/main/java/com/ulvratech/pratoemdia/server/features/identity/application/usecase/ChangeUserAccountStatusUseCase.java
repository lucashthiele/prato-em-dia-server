package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangeUserAccountStatusInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountNotFoundException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangeUserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class ChangeUserAccountStatusUseCase implements ChangeUserAccountStatus {
    private final UserAccountRepository accounts;
    private final RefreshTokenRepository tokens;
    private final Clock clock;

    public ChangeUserAccountStatusUseCase(UserAccountRepository accounts, RefreshTokenRepository tokens, Clock clock) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public UserAccountStatus execute(ChangeUserAccountStatusInput input) {
        var account = accounts.findById(input.userAccountId()).orElseThrow(UserAccountNotFoundException::new);
        var previous = account.getStatus();
        if (input.status() == UserAccountStatus.INACTIVE) {
            account.inactivate();
            tokens.revokeAllByUserAccountId(account.getId(), clock.instant());
        } else {
            account.activate();
        }
        accounts.save(account);
        return previous;
    }
}

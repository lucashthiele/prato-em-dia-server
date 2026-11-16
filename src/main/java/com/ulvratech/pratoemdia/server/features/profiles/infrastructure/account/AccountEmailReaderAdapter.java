package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.account;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter.UserAccountQueryAdapter;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.out.AccountEmailReader;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class AccountEmailReaderAdapter implements AccountEmailReader {
    private final UserAccountQueryAdapter accounts;

    AccountEmailReaderAdapter(UserAccountQueryAdapter accounts) {
        this.accounts = accounts;
    }

    @Override
    public String email(UUID userAccountId) {
        return accounts.email(userAccountId);
    }
}

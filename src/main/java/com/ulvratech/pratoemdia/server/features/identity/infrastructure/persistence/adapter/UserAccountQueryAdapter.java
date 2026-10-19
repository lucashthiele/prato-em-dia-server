package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.UserAccountModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Read-only view of account data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class UserAccountQueryAdapter {
    private final UserAccountJpaRepository accounts;

    UserAccountQueryAdapter(UserAccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    public String email(UUID accountId) {
        return accounts.findById(accountId)
                .map(UserAccountModel::getEmail)
                .orElseThrow(() -> new IllegalStateException("User account not found: " + accountId));
    }
}

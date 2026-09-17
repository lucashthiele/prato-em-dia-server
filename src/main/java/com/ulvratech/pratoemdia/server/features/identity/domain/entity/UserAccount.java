package com.ulvratech.pratoemdia.server.features.identity.domain.entity;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

import java.util.UUID;

public class UserAccount {
    private final UserAccountRole role;
    private UUID id;
    private String email;
    private String passwordHash;
    private UserAccountStatus status;
    private boolean passwordChangeRequired;

    public UserAccount(
            UUID id,
            String email,
            String passwordHash,
            UserAccountRole role,
            UserAccountStatus status
    ) {
        this(id, email, passwordHash, role, status, false);
    }

    public UserAccount(
            UUID id,
            String email,
            String passwordHash,
            UserAccountRole role,
            UserAccountStatus status,
            boolean passwordChangeRequired
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public UserAccount(
            String email, String passwordHash, UserAccountRole role, UserAccountStatus status
    ) {
        this(null, email, passwordHash, role, status, false);
    }

    public UserAccount(
            String email,
            String passwordHash,
            UserAccountRole role,
            UserAccountStatus status,
            boolean passwordChangeRequired
    ) {
        this(null, email, passwordHash, role, status, passwordChangeRequired);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserAccountRole getRole() {
        return role;
    }

    public UserAccountStatus getStatus() {
        return status;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void replacePassword(String passwordHash, boolean changeRequired) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        this.passwordHash = passwordHash;
        this.passwordChangeRequired = changeRequired;
    }

    public void inactivate() {
        this.status = UserAccountStatus.INACTIVE;
    }

    public void activate() {
        this.status = UserAccountStatus.ACTIVE;
    }

    public void updateEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        this.email = email;
    }
}

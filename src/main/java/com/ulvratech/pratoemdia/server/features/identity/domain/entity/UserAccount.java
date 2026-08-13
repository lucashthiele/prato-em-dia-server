package com.ulvratech.pratoemdia.server.features.identity.domain.entity;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

import java.util.UUID;

public class UserAccount {
    private UUID id;
    private String email;
    private final String passwordHash;
    private final UserAccountRole role;
    private UserAccountStatus status;

    public UserAccount(UUID id, String email, String passwordHash, UserAccountRole role, UserAccountStatus status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public UserAccount(String email, String passwordHash, UserAccountRole role, UserAccountStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
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

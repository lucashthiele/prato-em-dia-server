package com.ulvratech.pratoemdia.server.features.identity.domain.entity;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

import java.util.UUID;

public class UserAccount {
    private final UUID id;
    private String email;
    private String passwordHash;
    private UserAccountRole role;
    private UserAccountStatus status;

    public UserAccount(UUID id, String email, String passwordHash, UserAccountRole role, UserAccountStatus status) {
        this.id = id;
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
}

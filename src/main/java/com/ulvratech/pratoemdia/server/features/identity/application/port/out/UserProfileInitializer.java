package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;

import java.util.UUID;

public interface UserProfileInitializer {
    default void initialize(UUID userAccountId, UserAccountRole role, String fullName) {
        initialize(userAccountId, role, fullName, null);
    }

    /** {@code registrationNumber} (professional registry) only applies to nutritionists and may be null. */
    void initialize(UUID userAccountId, UserAccountRole role, String fullName, String registrationNumber);
}

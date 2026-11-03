package com.ulvratech.pratoemdia.server.features.admin.application.dto;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

import java.time.Instant;
import java.util.UUID;

/** Read model of one account for the admin screens. {@code accessEmail} is null when it does not apply. */
public record AdminUserView(
        UUID id,
        String email,
        UserAccountRole role,
        UserAccountStatus status,
        boolean passwordChangeRequired,
        String fullName,
        String registrationNumber,
        Instant createdAt,
        AccessEmail accessEmail) {
    /** Delivery state of the access email: from the outbox for nutritionists, from the relationship for patients. */
    public record AccessEmail(String status, int attempts, String lastError) {
    }
}

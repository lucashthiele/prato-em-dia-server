package com.ulvratech.pratoemdia.server.features.profiles.application.dto;

import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileOutput(
        UUID id,
        UUID userAccountId,
        String role,
        String email,
        String fullName,
        String phone,
        String registrationNumber,
        String bio,
        LocalDate birthDate,
        Instant createdAt,
        Instant updatedAt) {
    public static final String ADMIN_ROLE = "ADMIN";

    public static ProfileOutput from(UserProfile profile, String email) {
        return new ProfileOutput(
                profile.getId(), profile.getUserAccountId(), profile.getType().name(), email,
                profile.getFullName(), profile.getPhone(), profile.getRegistrationNumber(),
                profile.getBio(), profile.getBirthDate(), profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    /** Conta ADMIN não tem perfil: só a identificação da conta. */
    public static ProfileOutput admin(UUID userAccountId, String email) {
        return new ProfileOutput(
                null, userAccountId, ADMIN_ROLE, email, null, null, null, null, null, null, null
        );
    }
}

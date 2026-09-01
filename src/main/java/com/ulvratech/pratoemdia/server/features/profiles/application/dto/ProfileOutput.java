package com.ulvratech.pratoemdia.server.features.profiles.application.dto;

import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileOutput(
    UUID id,
    UUID userAccountId,
    String role,
    String fullName,
    String phone,
    String registrationNumber,
    String bio,
    LocalDate birthDate,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProfileOutput from(UserProfile profile) {
        return new ProfileOutput(
            profile.getId(), profile.getUserAccountId(), profile.getType().name(),
            profile.getFullName(), profile.getPhone(), profile.getRegistrationNumber(),
            profile.getBio(), profile.getBirthDate(), profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}

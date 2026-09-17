package com.ulvratech.pratoemdia.server.features.profiles.presentation.web.response;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CurrentProfileResponse(
        UUID id,
        UUID userAccountId,
        String role,
        String fullName,
        String phone,
        String registrationNumber,
        String bio,
        LocalDate birthDate,
        Instant createdAt,
        Instant updatedAt) {
    public static CurrentProfileResponse from(ProfileOutput output) {
        return new CurrentProfileResponse(
                output.id(), output.userAccountId(), output.role(), output.fullName(),
                output.phone(), output.registrationNumber(), output.bio(),
                output.birthDate(), output.createdAt(), output.updatedAt()
        );
    }
}

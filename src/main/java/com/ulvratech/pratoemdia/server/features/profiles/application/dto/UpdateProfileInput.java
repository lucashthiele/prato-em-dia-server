package com.ulvratech.pratoemdia.server.features.profiles.application.dto;

import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateProfileInput(
    UUID userAccountId,
    ProfileType type,
    String fullName,
    String phone,
    String registrationNumber,
    String bio,
    LocalDate birthDate) {}

package com.ulvratech.pratoemdia.server.features.profiles.application.dto;

import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;

import java.util.UUID;

public record GetProfileInput(UUID userAccountId, ProfileType type) {
}

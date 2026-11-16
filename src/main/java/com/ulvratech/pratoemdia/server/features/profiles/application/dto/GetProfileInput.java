package com.ulvratech.pratoemdia.server.features.profiles.application.dto;

import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;

import java.util.UUID;

/** {@code type} nulo é uma conta sem perfil (ADMIN). */
public record GetProfileInput(UUID userAccountId, ProfileType type) {
}

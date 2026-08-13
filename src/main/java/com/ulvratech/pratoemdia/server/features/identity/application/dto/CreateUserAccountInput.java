package com.ulvratech.pratoemdia.server.features.identity.application.dto;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;

public record CreateUserAccountInput(
    String email,
    String rawPassword,
    UserAccountRole role
) {
}

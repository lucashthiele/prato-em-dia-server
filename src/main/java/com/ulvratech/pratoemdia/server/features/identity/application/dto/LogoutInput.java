package com.ulvratech.pratoemdia.server.features.identity.application.dto;

import java.util.UUID;

public record LogoutInput(
        UUID userAccountId,
        String refreshToken) {
}

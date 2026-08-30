package com.ulvratech.pratoemdia.server.features.identity.application.dto;

public record RefreshAccessTokenOutput(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
}

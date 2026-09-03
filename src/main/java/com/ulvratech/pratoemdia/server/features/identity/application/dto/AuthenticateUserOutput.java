package com.ulvratech.pratoemdia.server.features.identity.application.dto;

public record AuthenticateUserOutput(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    boolean passwordChangeRequired) {
}

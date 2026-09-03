package com.ulvratech.pratoemdia.server.features.identity.presentation.web.response;

public record AuthenticationResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    boolean passwordChangeRequired) {
}

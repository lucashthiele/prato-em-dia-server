package com.ulvratech.pratoemdia.server.features.identity.application.dto;

/** {@code ip} is the client address used by the login rate limit; it may be null when unknown. */
public record AuthenticateUserInput(String email, String rawPassword, String ip) {
    public AuthenticateUserInput(String email, String rawPassword) {
        this(email, rawPassword, null);
    }
}

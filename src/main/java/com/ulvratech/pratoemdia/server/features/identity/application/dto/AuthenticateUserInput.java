package com.ulvratech.pratoemdia.server.features.identity.application.dto;

/**
 * {@code ip} is the client address used by the login rate limit; it may be null when unknown.
 * {@code clientPlatform} is the lowercase platform of {@code X-Client-Version} ({@code web}, {@code android},
 * {@code ios}); it is null when the header is absent or malformed.
 */
public record AuthenticateUserInput(String email, String rawPassword, String ip, String clientPlatform) {
    public AuthenticateUserInput(String email, String rawPassword, String ip) {
        this(email, rawPassword, ip, null);
    }

    public AuthenticateUserInput(String email, String rawPassword) {
        this(email, rawPassword, null, null);
    }
}

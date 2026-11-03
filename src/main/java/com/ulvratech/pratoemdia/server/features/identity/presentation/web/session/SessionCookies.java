package com.ulvratech.pratoemdia.server.features.identity.presentation.web.session;

import com.ulvratech.pratoemdia.server.features.identity.config.JwtConfigurationProperties;
import com.ulvratech.pratoemdia.server.features.identity.config.SessionCookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Builds the HttpOnly session cookies described in ADR 0001. */
@Component
public class SessionCookies {
    public static final String ACCESS_COOKIE = SessionCookieProperties.ACCESS_COOKIE;
    public static final String REFRESH_COOKIE = SessionCookieProperties.REFRESH_COOKIE;
    public static final String REFRESH_PATH = "/api/v1/auth";

    private final SessionCookieProperties properties;
    private final JwtConfigurationProperties jwt;

    public SessionCookies(SessionCookieProperties properties, JwtConfigurationProperties jwt) {
        this.properties = properties;
        this.jwt = jwt;
    }

    public ResponseCookie access(String token, long expiresInSeconds) {
        return build(ACCESS_COOKIE, token, "/", Duration.ofSeconds(expiresInSeconds));
    }

    public ResponseCookie refresh(String token) {
        return build(REFRESH_COOKIE, token, REFRESH_PATH, jwt.refreshTokenTtl());
    }

    public List<ResponseCookie> clear() {
        return List.of(
                build(ACCESS_COOKIE, "", "/", Duration.ZERO),
                build(REFRESH_COOKIE, "", REFRESH_PATH, Duration.ZERO)
        );
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}

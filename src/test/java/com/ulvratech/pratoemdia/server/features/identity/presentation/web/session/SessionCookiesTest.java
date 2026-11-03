package com.ulvratech.pratoemdia.server.features.identity.presentation.web.session;

import com.ulvratech.pratoemdia.server.features.identity.config.JwtConfigurationProperties;
import com.ulvratech.pratoemdia.server.features.identity.config.SessionCookieProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCookiesTest {
    private final JwtConfigurationProperties jwt = new JwtConfigurationProperties(
            "prato-em-dia", Duration.ofMinutes(15), Duration.ofDays(30), "01234567890123456789012345678901"
    );

    @Test
    void accessCookieIsHttpOnlySecureStrictAndValidForTheAccessTokenLifetime() {
        var cookies = new SessionCookies(new SessionCookieProperties(true, List.of()), jwt);

        var cookie = cookies.access("jwt-value", 900);

        assertEquals("access_token", cookie.getName());
        assertEquals("jwt-value", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofSeconds(900), cookie.getMaxAge());
    }

    @Test
    void refreshCookieIsRestrictedToTheAuthPathAndLivesAsLongAsTheRefreshToken() {
        var cookies = new SessionCookies(new SessionCookieProperties(true, List.of()), jwt);

        var cookie = cookies.refresh("refresh-value");

        assertEquals("refresh_token", cookie.getName());
        assertEquals("/api/v1/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofDays(30), cookie.getMaxAge());
    }

    @Test
    void secureFlagFollowsConfigurationSoLocalHttpStillWorks() {
        var cookies = new SessionCookies(new SessionCookieProperties(false, List.of()), jwt);

        assertEquals(false, cookies.access("a", 10).isSecure());
        assertEquals(false, cookies.refresh("r").isSecure());
    }

    @Test
    void clearingExpiresBothCookiesOnTheirOriginalPaths() {
        var cookies = new SessionCookies(new SessionCookieProperties(true, List.of()), jwt);

        var cleared = cookies.clear();

        assertEquals(2, cleared.size());
        assertEquals("/", cleared.get(0).getPath());
        assertEquals("/api/v1/auth", cleared.get(1).getPath());
        cleared.forEach(c -> {
            assertEquals(Duration.ZERO, c.getMaxAge());
            assertEquals("", c.getValue());
        });
    }
}

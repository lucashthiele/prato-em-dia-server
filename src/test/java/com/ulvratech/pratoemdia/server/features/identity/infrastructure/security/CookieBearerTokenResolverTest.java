package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CookieBearerTokenResolverTest {
    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

    @Test
    void readsTheAccessTokenFromTheCookie() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "jwt-value"));

        assertEquals("jwt-value", resolver.resolve(request));
    }

    @Test
    void ignoresTheAuthorizationHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");

        assertNull(resolver.resolve(request));
    }

    @Test
    void ignoresBlankCookiesAndOtherCookies() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", ""), new Cookie("refresh_token", "x"));

        assertNull(resolver.resolve(request));
    }
}

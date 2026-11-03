package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.config.SessionCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/** Reads the access token only from the HttpOnly cookie; the Authorization header is ignored. */
public class CookieBearerTokenResolver implements BearerTokenResolver {
    @Override
    public String resolve(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SessionCookieProperties.ACCESS_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

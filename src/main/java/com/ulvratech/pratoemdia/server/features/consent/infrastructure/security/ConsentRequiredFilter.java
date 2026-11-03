package com.ulvratech.pratoemdia.server.features.consent.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.consent.application.port.in.GetConsentStatus;
import com.ulvratech.pratoemdia.server.features.consent.config.ConsentProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Blocks patients and nutritionists with {@code 403 CONSENT_REQUIRED} until they accepted the
 * current version of every legal document. Same style as the password-change filter. Registered as
 * a servlet filter ordered after the Spring Security chain, so the authentication is already set.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ConsentRequiredFilter extends OncePerRequestFilter {
    private static final Set<String> SUBJECT_ROLES = Set.of("PATIENT", "NUTRITIONIST");
    private static final Set<String> ALLOWED = Set.of(
            "GET /api/v1/me/consents",
            "POST /api/v1/me/consents",
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/refresh",
            "PATCH /api/v1/me/password"
    );

    private final GetConsentStatus consentStatus;
    private final ConsentProperties properties;

    public ConsentRequiredFilter(GetConsentStatus consentStatus, ConsentProperties properties) {
        this.consentStatus = consentStatus;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (properties.enforced()
                && authentication instanceof JwtAuthenticationToken jwt
                && SUBJECT_ROLES.contains(jwt.getToken().getClaimAsString("role"))
                && !ALLOWED.contains(request.getMethod() + " " + request.getRequestURI())
                && !consentStatus.execute(UUID.fromString(jwt.getToken().getSubject())).upToDate()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":403,\"code\":\"CONSENT_REQUIRED\",\"message\":\"Aceite os termos de uso e a"
                            + " política de privacidade vigentes antes de continuar.\",\"errors\":[]}"
            );
            return;
        }
        chain.doFilter(request, response);
    }
}

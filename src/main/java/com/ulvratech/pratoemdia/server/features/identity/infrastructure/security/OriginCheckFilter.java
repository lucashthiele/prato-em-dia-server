package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.config.SessionCookieProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * CSRF defence for cookie sessions (ADR 0001): writes that carry an {@code Origin} header must
 * come from an allowed origin. Requests without {@code Origin} (native app, curl) pass, because
 * browsers always send it on cross-origin writes.
 */
@Component
public class OriginCheckFilter extends OncePerRequestFilter {
    private static final Set<String> STATE_CHANGING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final Set<String> allowedOrigins;

    public OriginCheckFilter(SessionCookieProperties properties) {
        this.allowedOrigins = Set.copyOf(properties.allowedOrigins());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {
        var origin = request.getHeader("Origin");
        if (origin != null
                && STATE_CHANGING.contains(request.getMethod())
                && !allowedOrigins.contains(origin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":403,\"code\":\"ORIGIN_NOT_ALLOWED\",\"message\":\"Origem não permitida"
                            + " para esta operação.\",\"errors\":[]}"
            );
            return;
        }
        chain.doFilter(request, response);
    }
}

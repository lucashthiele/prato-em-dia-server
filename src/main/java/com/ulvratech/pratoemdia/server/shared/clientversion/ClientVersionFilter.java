package com.ulvratech.pratoemdia.server.shared.clientversion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Rejects clients older than the configured minimum for their platform with
 * {@code 426 CLIENT_OUTDATED}. Requests without a parseable {@code X-Client-Version}, or from a
 * platform without a configured minimum, pass through.
 */
@Component
public class ClientVersionFilter extends OncePerRequestFilter {
    private final ClientVersionProperties properties;

    public ClientVersionFilter(ClientVersionProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {
        var client = ClientVersion.parse(request.getHeader(ClientVersion.HEADER));
        if (client.isPresent()) {
            var minimum = properties.minVersions().get(client.get().platform());
            if (minimum != null && client.get().isOlderThan(minimum)) {
                response.setStatus(426);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"status\":426,\"code\":\"CLIENT_OUTDATED\",\"message\":\"Esta versão do aplicativo"
                                + " não é mais suportada. Atualize para a versão " + minimum
                                + " ou superior.\",\"errors\":[]}"
                );
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

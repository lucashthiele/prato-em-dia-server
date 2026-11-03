package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.config.SessionCookieProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginCheckFilterTest {
    private final OriginCheckFilter filter = new OriginCheckFilter(
            new SessionCookieProperties(true, List.of("https://portal.example.com"))
    );

    private MockHttpServletResponse run(String method, String origin) throws Exception {
        var request = new MockHttpServletRequest(method, "/api/v1/auth/login");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (response.getStatus() == 200) {
            assertNotNull(chain.getRequest(), "request should have continued");
        } else {
            assertNull(chain.getRequest(), "request should have been blocked");
        }
        return response;
    }

    @Test
    void stateChangingRequestFromAForeignOriginIsForbidden() throws Exception {
        for (var method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            var response = run(method, "https://evil.example.com");
            assertEquals(403, response.getStatus());
            assertTrue(response.getContentAsString().contains("\"code\":\"ORIGIN_NOT_ALLOWED\""));
        }
    }

    @Test
    void allowedOriginsPass() throws Exception {
        assertEquals(200, run("POST", "https://portal.example.com").getStatus());
    }

    @Test
    void requestsWithoutOriginPass_nativeAppsDoNotSendIt() throws Exception {
        assertEquals(200, run("POST", null).getStatus());
    }

    @Test
    void safeMethodsAreNeverChecked() throws Exception {
        assertEquals(200, run("GET", "https://evil.example.com").getStatus());
    }

    @Test
    void opaqueNullOriginIsRejectedOnWrites() throws Exception {
        assertEquals(403, run("POST", "null").getStatus());
    }
}

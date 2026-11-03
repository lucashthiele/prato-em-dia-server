package com.ulvratech.pratoemdia.server.features.consent.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.ConsentStatusOutput;
import com.ulvratech.pratoemdia.server.features.consent.application.port.in.GetConsentStatus;
import com.ulvratech.pratoemdia.server.features.consent.config.ConsentProperties;
import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsentRequiredFilterTest {
    private final GetConsentStatus status = mock(GetConsentStatus.class);
    private final ConsentRequiredFilter filter = filterWith(true);

    private ConsentRequiredFilter filterWith(boolean enforced) {
        return new ConsentRequiredFilter(
                status,
                new ConsentProperties(
                        enforced,
                        java.util.Map.of(ConsentDocumentType.TERMS_OF_USE, "1", ConsentDocumentType.PRIVACY_POLICY, "1")
                )
        );
    }

    private final UUID user = UUID.randomUUID();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        var jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString()).claim("role", role)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void pending(boolean upToDate) {
        when(status.execute(any())).thenReturn(new ConsentStatusOutput(upToDate, List.of()));
    }

    private MockHttpServletResponse call(String method, String uri) throws Exception {
        var request = new MockHttpServletRequest(method, uri);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (response.getStatus() == 200) {
            assertNotNull(chain.getRequest());
        } else {
            assertNull(chain.getRequest());
        }
        return response;
    }

    @Test
    void blocksPatientAndNutritionistUntilTheCurrentVersionsAreAccepted() throws Exception {
        pending(false);
        for (var role : List.of("PATIENT", "NUTRITIONIST")) {
            authenticateAs(role);
            var response = call("GET", "/api/v1/patients/me/weekly-plans");
            assertEquals(403, response.getStatus());
            assertTrue(response.getContentAsString().contains("\"code\":\"CONSENT_REQUIRED\""));
        }
    }

    @Test
    void letsRequestsThroughOnceAccepted() throws Exception {
        pending(true);
        authenticateAs("PATIENT");

        assertEquals(200, call("GET", "/api/v1/patients/me/weekly-plans").getStatus());
    }

    @Test
    void acceptanceRoutesLogoutRefreshAndPasswordChangeStayOpen() throws Exception {
        pending(false);
        authenticateAs("PATIENT");

        assertEquals(200, call("GET", "/api/v1/me/consents").getStatus());
        assertEquals(200, call("POST", "/api/v1/me/consents").getStatus());
        assertEquals(200, call("POST", "/api/v1/auth/logout").getStatus());
        assertEquals(200, call("POST", "/api/v1/auth/refresh").getStatus());
        assertEquals(200, call("PATCH", "/api/v1/me/password").getStatus());
    }

    @Test
    void adminAndAnonymousRequestsAreNotChecked() throws Exception {
        authenticateAs("ADMIN");
        assertEquals(200, call("GET", "/api/v1/admin/users").getStatus());
        SecurityContextHolder.clearContext();
        assertEquals(200, call("POST", "/api/v1/auth/login").getStatus());
        verifyNoInteractions(status);
    }

    @Test
    void doesNothingWhenEnforcementIsOff() throws Exception {
        var off = filterWith(false);
        authenticateAs("PATIENT");
        var request = new MockHttpServletRequest("GET", "/api/v1/patients/me/weekly-plans");
        var response = new MockHttpServletResponse();

        off.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        verifyNoInteractions(status);
    }
}

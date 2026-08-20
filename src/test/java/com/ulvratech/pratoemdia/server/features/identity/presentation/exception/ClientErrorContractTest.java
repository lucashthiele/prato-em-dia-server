package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientErrorContractTest {
    @Test
    void applicationErrorsExposeAStableCodeAndPortugueseFallback() {
        var handler = new IdentityExceptionHandler();

        var response = handler.handleAuthenticationFailure(
            new InvalidCredentialsException()
        );

        assertEquals(401, response.getStatusCode().value());
        assertEquals("AUTH_INVALID_CREDENTIALS", response.getBody().code());
        assertEquals("E-mail ou senha inválidos.", response.getBody().message());
        assertTrue(response.getBody().errors().isEmpty());
    }

    @Test
    void unauthenticatedRequestsUseTheSameErrorContract() throws Exception {
        var handler = new SecurityErrorResponseHandler();
        var response = new MockHttpServletResponse();

        handler.commence(
            new MockHttpServletRequest(),
            response,
            new BadCredentialsException("ignored")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains(
            "\"code\":\"AUTHENTICATION_REQUIRED\""
        ));
        assertTrue(response.getContentAsString().contains("Autenticação necessária"));
    }

    @Test
    void forbiddenRequestsUseTheSameErrorContract() throws Exception {
        var handler = new SecurityErrorResponseHandler();
        var response = new MockHttpServletResponse();

        handler.handle(
            new MockHttpServletRequest(),
            response,
            new AccessDeniedException("ignored")
        );

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains(
            "\"code\":\"ACCESS_DENIED\""
        ));
        assertTrue(response.getContentAsString().contains("Você não tem permissão"));
    }
}

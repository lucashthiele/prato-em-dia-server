package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException;
import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void unhandledApplicationErrorsDoNotExposeInternalDetails() {
        var handler = new IdentityExceptionHandler();
        var exception = new TestApplicationException();

        var response = handler.handleUnhandledApplicationFailure(exception);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals(
            "Não foi possível concluir a operação. Tente novamente mais tarde.",
            response.getBody().message()
        );
        assertTrue(response.getBody().errors().isEmpty());
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalDetails() {
        var handler = new IdentityExceptionHandler();

        var response = handler.handleUnexpectedFailure(
            new IllegalStateException("database password leaked")
        );

        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertFalse(response.getBody().message().contains("database password leaked"));
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

    private static final class TestApplicationException
        extends CodedApplicationException {

        private TestApplicationException() {
            super("TEST_INTERNAL_FAILURE");
        }
    }
}

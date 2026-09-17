package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.*;
import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ClientErrorContractTest {
  @Test
  void applicationErrorsExposeAStableCodeAndPortugueseFallback() {
    var handler = new IdentityExceptionHandler();

    var response = handler.handleAuthenticationFailure(new InvalidCredentialsException());

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
        response.getBody().message());
    assertTrue(response.getBody().errors().isEmpty());
  }

  @Test
  void unexpectedErrorsDoNotExposeInternalDetails() {
    var handler = new IdentityExceptionHandler();

    var response =
        handler.handleUnexpectedFailure(new IllegalStateException("database password leaked"));

    assertEquals(500, response.getStatusCode().value());
    assertEquals("INTERNAL_ERROR", response.getBody().code());
    assertFalse(response.getBody().message().contains("database password leaked"));
  }

  @Test
  void mapsIdentityConflictsAndEveryAuthenticationFailure() {
    var handler = new IdentityExceptionHandler();

    assertEquals(
        409,
        handler
            .handleUserAccountAlreadyExists(new UserAccountAlreadyExistsException())
            .getStatusCode()
            .value());
    assertEquals(
        409,
        handler
            .handlePatientAccountConflict(new PatientAccountConflictException())
            .getStatusCode()
            .value());
    assertEquals(
        "Esta conta está inativa.",
        handler
            .handleAuthenticationFailure(new InactiveUserAccountException())
            .getBody()
            .message());
    assertEquals(
        "O token de renovação é inválido ou foi revogado.",
        handler
            .handleAuthenticationFailure(new InvalidRefreshTokenException())
            .getBody()
            .message());
    assertEquals(
        "O token de renovação expirou.",
        handler
            .handleAuthenticationFailure(new ExpiredRefreshTokenException())
            .getBody()
            .message());
  }

  @Test
  void mapsFrameworkRequestErrors() {
    var handler = new IdentityExceptionHandler();

    assertEquals("INVALID_REQUEST_BODY", handler.handleUnreadableBody().getBody().code());
    assertEquals(
        "INVALID_REQUEST_PARAMETER", handler.handleInvalidRequestParameter().getBody().code());
    assertEquals(404, handler.handleRouteNotFound().getStatusCode().value());
    assertEquals(405, handler.handleMethodNotAllowed().getStatusCode().value());
    assertEquals(415, handler.handleUnsupportedMediaType().getStatusCode().value());
  }

  @Test
  void mapsBeanValidationCodesAndFallbacks() {
    BindingResult result = mock(BindingResult.class);
    var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), result);
    when(result.getFieldErrors())
        .thenReturn(
            List.of(
                new FieldError("request", "email", null, false, new String[] {"Email"}, null, null),
                new FieldError(
                    "request", "name", null, false, new String[] {"NotBlank"}, null, null),
                new FieldError("request", "size", null, false, new String[] {"Size"}, null, null),
                new FieldError(
                    "request", "format", null, false, new String[] {"Pattern"}, null, null),
                new FieldError("request", "other", null, false, new String[] {"Other"}, null, null),
                new FieldError("request", "unknown", null, false, null, null, null)));

    var response = new IdentityExceptionHandler().handleValidationFailure(exception);

    assertEquals(400, response.getStatusCode().value());
    assertEquals(6, response.getBody().errors().size());
    assertEquals("INVALID_EMAIL", response.getBody().errors().get(0).code());
    assertEquals("REQUIRED_FIELD", response.getBody().errors().get(1).code());
    assertEquals("INVALID_SIZE", response.getBody().errors().get(2).code());
    assertEquals("INVALID_FORMAT", response.getBody().errors().get(3).code());
    assertEquals("INVALID_VALUE", response.getBody().errors().get(5).code());
  }

  @Test
  void unauthenticatedRequestsUseTheSameErrorContract() throws Exception {
    var handler = new SecurityErrorResponseHandler();
    var response = new MockHttpServletResponse();

    handler.commence(
        new MockHttpServletRequest(), response, new BadCredentialsException("ignored"));

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentType().startsWith("application/json"));
    assertTrue(response.getContentAsString().contains("\"code\":\"AUTHENTICATION_REQUIRED\""));
    assertTrue(response.getContentAsString().contains("Autenticação necessária"));
  }

  @Test
  void forbiddenRequestsUseTheSameErrorContract() throws Exception {
    var handler = new SecurityErrorResponseHandler();
    var response = new MockHttpServletResponse();

    handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("ignored"));

    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"code\":\"ACCESS_DENIED\""));
    assertTrue(response.getContentAsString().contains("Você não tem permissão"));
  }

  private static final class TestApplicationException extends CodedApplicationException {

    private TestApplicationException() {
      super("TEST_INTERNAL_FAILURE");
    }
  }
}

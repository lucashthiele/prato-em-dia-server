package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    write(
        response,
        HttpStatus.UNAUTHORIZED,
        "AUTHENTICATION_REQUIRED",
        "Autenticação necessária para acessar este recurso.");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    write(
        response,
        HttpStatus.FORBIDDEN,
        "ACCESS_DENIED",
        "Você não tem permissão para realizar esta operação.");
  }

  private void write(HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            """
            {"status":%d,"code":"%s","message":"%s","errors":[]}
            """
                .formatted(status.value(), code, message)
                .strip());
  }
}

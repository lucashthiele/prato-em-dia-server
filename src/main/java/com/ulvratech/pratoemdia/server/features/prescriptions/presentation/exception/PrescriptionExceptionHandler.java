package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.InvalidPrescriptionReferenceException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionConflictException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionNotFoundException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    basePackages = "com.ulvratech.pratoemdia.server.features.prescriptions.presentation")
public class PrescriptionExceptionHandler {
  @ExceptionHandler(PrescriptionNotFoundException.class)
  ResponseEntity<ApiErrorResponse> notFound(PrescriptionNotFoundException exception) {
    return response(
        HttpStatus.NOT_FOUND, exception.getCode(), "A prescrição solicitada não foi encontrada.");
  }

  @ExceptionHandler(PrescriptionAccessDeniedException.class)
  ResponseEntity<ApiErrorResponse> denied(PrescriptionAccessDeniedException exception) {
    return response(
        HttpStatus.FORBIDDEN, exception.getCode(), "Você não tem acesso a esta prescrição.");
  }

  @ExceptionHandler(PrescriptionConflictException.class)
  ResponseEntity<ApiErrorResponse> conflict(PrescriptionConflictException exception) {
    return response(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler(InvalidPrescriptionReferenceException.class)
  ResponseEntity<ApiErrorResponse> invalidReference(
      InvalidPrescriptionReferenceException exception) {
    return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> invalid(IllegalArgumentException exception) {
    return response(HttpStatus.BAD_REQUEST, "INVALID_PRESCRIPTION", exception.getMessage());
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status, String code, String message) {
    return ResponseEntity.status(status)
        .body(new ApiErrorResponse(status.value(), code, message, List.of()));
  }
}

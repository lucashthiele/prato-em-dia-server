package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.InvalidPrescriptionReferenceException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionConflictException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionNotFoundException;
import org.junit.jupiter.api.Test;

class PrescriptionExceptionHandlerTest {
  @Test
  void mapsPrescriptionErrors() {
    var handler = new PrescriptionExceptionHandler();
    assertEquals(
        404, handler.notFound(new PrescriptionNotFoundException()).getStatusCode().value());
    assertEquals(
        403, handler.denied(new PrescriptionAccessDeniedException()).getStatusCode().value());
    assertEquals(
        409,
        handler.conflict(new PrescriptionConflictException("conflict")).getStatusCode().value());
    assertEquals(
        422,
        handler
            .invalidReference(new InvalidPrescriptionReferenceException("invalid"))
            .getStatusCode()
            .value());
    assertEquals(
        "INVALID_PRESCRIPTION",
        handler.invalid(new IllegalArgumentException("bad")).getBody().code());
  }
}

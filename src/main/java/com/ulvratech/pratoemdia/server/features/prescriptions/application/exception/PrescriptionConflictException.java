package com.ulvratech.pratoemdia.server.features.prescriptions.application.exception;

public class PrescriptionConflictException extends RuntimeException {
  public PrescriptionConflictException(String message) {
    super(message);
  }

  public String getCode() {
    return "PRESCRIPTION_CONFLICT";
  }
}

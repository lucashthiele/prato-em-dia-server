package com.ulvratech.pratoemdia.server.features.prescriptions.application.exception;

public class PrescriptionNotFoundException extends RuntimeException {
  public String getCode() {
    return "PRESCRIPTION_NOT_FOUND";
  }
}

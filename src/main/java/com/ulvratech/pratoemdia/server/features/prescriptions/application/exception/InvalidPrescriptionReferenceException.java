package com.ulvratech.pratoemdia.server.features.prescriptions.application.exception;

public class InvalidPrescriptionReferenceException extends RuntimeException {
    public InvalidPrescriptionReferenceException(String message) {
        super(message);
    }

    public String getCode() {
        return "INVALID_PRESCRIPTION_REFERENCE";
    }
}

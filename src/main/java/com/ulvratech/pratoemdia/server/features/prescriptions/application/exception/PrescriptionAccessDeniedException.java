package com.ulvratech.pratoemdia.server.features.prescriptions.application.exception;

public class PrescriptionAccessDeniedException extends RuntimeException {
    public String getCode() {
        return "PRESCRIPTION_ACCESS_DENIED";
    }
}

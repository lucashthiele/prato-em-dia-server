package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class PatientAccountConflictException extends CodedApplicationException {
    public PatientAccountConflictException() {
        super("PATIENT_ACCOUNT_CONFLICT");
    }
}

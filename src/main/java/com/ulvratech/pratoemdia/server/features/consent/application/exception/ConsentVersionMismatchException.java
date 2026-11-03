package com.ulvratech.pratoemdia.server.features.consent.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ConsentVersionMismatchException extends CodedApplicationException {
    public ConsentVersionMismatchException() {
        super("CONSENT_VERSION_MISMATCH");
    }
}

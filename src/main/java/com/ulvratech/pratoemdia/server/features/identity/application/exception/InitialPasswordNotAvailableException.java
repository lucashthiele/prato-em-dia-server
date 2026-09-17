package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class InitialPasswordNotAvailableException extends CodedApplicationException {
    public InitialPasswordNotAvailableException() {
        super("INITIAL_PASSWORD_NOT_AVAILABLE");
    }
}

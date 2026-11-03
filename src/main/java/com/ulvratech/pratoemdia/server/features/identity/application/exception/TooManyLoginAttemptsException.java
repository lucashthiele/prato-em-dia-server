package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class TooManyLoginAttemptsException extends CodedApplicationException {
    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("LOGIN_RATE_LIMITED");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

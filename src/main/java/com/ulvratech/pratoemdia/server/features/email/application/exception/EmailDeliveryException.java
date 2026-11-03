package com.ulvratech.pratoemdia.server.features.email.application.exception;

/** Provider failure. {@code transientFailure} is true for timeouts, 5xx and 429 (worth retrying). */
public class EmailDeliveryException extends RuntimeException {
    private final boolean transientFailure;

    public EmailDeliveryException(String message, boolean transientFailure) {
        super(message);
        this.transientFailure = transientFailure;
    }

    public EmailDeliveryException(String message, boolean transientFailure, Throwable cause) {
        super(message, cause);
        this.transientFailure = transientFailure;
    }

    public boolean isTransientFailure() {
        return transientFailure;
    }
}

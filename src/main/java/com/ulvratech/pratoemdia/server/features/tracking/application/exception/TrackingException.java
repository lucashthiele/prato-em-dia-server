package com.ulvratech.pratoemdia.server.features.tracking.application.exception;

public class TrackingException extends RuntimeException {
    public enum Kind {
        NOT_FOUND, FORBIDDEN, CONFLICT, INVALID
    }

    private final Kind kind;

    public TrackingException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}

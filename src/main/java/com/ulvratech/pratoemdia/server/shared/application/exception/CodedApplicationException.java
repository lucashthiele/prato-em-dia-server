package com.ulvratech.pratoemdia.server.shared.application.exception;

public abstract class CodedApplicationException extends RuntimeException {
    private final String code;

    protected CodedApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

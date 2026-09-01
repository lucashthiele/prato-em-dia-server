package com.ulvratech.pratoemdia.server.shared.application.exception;

public abstract class CodedApplicationException extends RuntimeException {
    private final String code;

    protected CodedApplicationException(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

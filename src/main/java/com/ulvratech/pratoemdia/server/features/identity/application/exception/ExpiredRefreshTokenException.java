package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ExpiredRefreshTokenException extends CodedApplicationException {
    public ExpiredRefreshTokenException() {
        super("AUTH_REFRESH_TOKEN_EXPIRED", "O token de renovação expirou.");
    }
}

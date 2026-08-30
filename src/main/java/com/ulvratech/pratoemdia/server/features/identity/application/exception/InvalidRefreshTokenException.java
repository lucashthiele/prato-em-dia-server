package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class InvalidRefreshTokenException extends CodedApplicationException {
    public InvalidRefreshTokenException() {
        super(
            "AUTH_REFRESH_TOKEN_INVALID",
            "O token de renovação é inválido ou foi revogado."
        );
    }
}

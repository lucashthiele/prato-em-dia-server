package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class UserAccountAlreadyExistsException extends CodedApplicationException {
    public UserAccountAlreadyExistsException(String email) {
        super(
            "IDENTITY_ACCOUNT_ALREADY_EXISTS",
            "Já existe uma conta cadastrada com este e-mail."
        );
    }
}

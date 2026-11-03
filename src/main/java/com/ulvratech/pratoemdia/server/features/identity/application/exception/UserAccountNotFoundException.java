package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class UserAccountNotFoundException extends CodedApplicationException {
    public UserAccountNotFoundException() {
        super("USER_ACCOUNT_NOT_FOUND");
    }
}

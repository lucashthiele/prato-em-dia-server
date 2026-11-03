package com.ulvratech.pratoemdia.server.features.admin.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

/** Access can only be resent to an active nutritionist who has not replaced the temporary password yet. */
public class AccessEmailNotAvailableException extends CodedApplicationException {
    public AccessEmailNotAvailableException() {
        super("ACCESS_EMAIL_NOT_AVAILABLE");
    }
}

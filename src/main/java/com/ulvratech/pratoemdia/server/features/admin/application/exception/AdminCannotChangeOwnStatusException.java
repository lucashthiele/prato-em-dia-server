package com.ulvratech.pratoemdia.server.features.admin.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class AdminCannotChangeOwnStatusException extends CodedApplicationException {
    public AdminCannotChangeOwnStatusException() {
        super("ADMIN_CANNOT_CHANGE_OWN_STATUS");
    }
}

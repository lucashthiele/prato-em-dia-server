package com.ulvratech.pratoemdia.server.features.relationships.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class RelationshipEmailException extends CodedApplicationException {
    public RelationshipEmailException() {
        super("RELATIONSHIP_EMAIL_FAILED");
    }
}

package com.ulvratech.pratoemdia.server.features.relationships.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

import java.util.UUID;

public class RelationshipAlreadyExistsException extends CodedApplicationException {
    private final UUID patientId;

    public RelationshipAlreadyExistsException(UUID patientId) {
        super("RELATIONSHIP_ALREADY_EXISTS");
        this.patientId = patientId;
    }

    public UUID getPatientId() {
        return patientId;
    }
}

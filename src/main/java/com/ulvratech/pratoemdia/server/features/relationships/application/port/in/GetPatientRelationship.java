package com.ulvratech.pratoemdia.server.features.relationships.application.port.in;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;

import java.util.UUID;

public interface GetPatientRelationship {
    RelationshipOutput execute(UUID patientAccountId);
}

package com.ulvratech.pratoemdia.server.features.relationships.application.port.out;

import java.util.UUID;

/**
 * Reaction to a relationship being ended, called in the same transaction by every end path
 * (nutritionist, patient, admin). Implemented by other features, so this feature does not depend on them.
 */
public interface RelationshipEndedHandler {
    void onRelationshipEnded(UUID nutritionistId, UUID patientId);
}

package com.ulvratech.pratoemdia.server.features.relationships.application.port.in;

import java.util.UUID;

/** Ends every active relationship of a nutritionist account on behalf of {@code actorAccountId}; returns how many. */
public interface EndAllNutritionistRelationships {
    int execute(UUID actorAccountId, UUID nutritionistAccountId);
}

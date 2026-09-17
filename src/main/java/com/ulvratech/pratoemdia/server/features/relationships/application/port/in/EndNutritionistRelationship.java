package com.ulvratech.pratoemdia.server.features.relationships.application.port.in;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;

public interface EndNutritionistRelationship {
  void execute(RelationshipQueryInput input);
}

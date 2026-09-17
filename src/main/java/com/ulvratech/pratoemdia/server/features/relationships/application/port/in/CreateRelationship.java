package com.ulvratech.pratoemdia.server.features.relationships.application.port.in;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface CreateRelationship extends UseCase<CreateRelationshipInput, RelationshipOutput> {
}

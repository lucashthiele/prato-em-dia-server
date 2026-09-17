package com.ulvratech.pratoemdia.server.features.relationships.application.port.in;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

public interface ListNutritionistPatients
    extends UseCase<ListRelationshipsInput, PageResult<RelationshipOutput>> {}

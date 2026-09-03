package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPatientRelationshipUseCase implements GetPatientRelationship {
    private final RelationshipRepository repository;

    public GetPatientRelationshipUseCase(RelationshipRepository r) {
        repository = r;
    }

    public RelationshipOutput execute(UUID id) {
        return repository
            .findActiveViewByPatientAccount(id)
            .map(RelationshipOutput::from)
            .orElseThrow(RelationshipNotFoundException::new);
    }
}

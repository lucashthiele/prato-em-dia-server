package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndNutritionistRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@Transactional
public class EndNutritionistRelationshipUseCase implements EndNutritionistRelationship {
    private final RelationshipRepository repository;
    private final Clock clock;

    public EndNutritionistRelationshipUseCase(RelationshipRepository r, Clock c) {
        repository = r;
        clock = c;
    }

    public void execute(RelationshipQueryInput i) {
        var view = repository
                .findActiveViewByNutritionistAccountAndPatient(i.accountId(), i.patientId())
                .orElseThrow(RelationshipConflictException::new);
        var relationship = repository
                .findById(view.relationshipId())
                .orElseThrow(RelationshipNotFoundException::new);
        relationship.end(i.accountId(), clock.instant());
        repository.save(relationship);
    }
}

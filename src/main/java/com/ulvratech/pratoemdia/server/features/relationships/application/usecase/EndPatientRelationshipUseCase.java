package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.RelationshipEndedHandler;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class EndPatientRelationshipUseCase implements EndPatientRelationship {
    private final RelationshipRepository repository;
    private final Clock clock;
    private final RelationshipEndedHandler archivePrescriptions;

    public EndPatientRelationshipUseCase(RelationshipRepository r, Clock c, RelationshipEndedHandler a) {
        repository = r;
        clock = c;
        archivePrescriptions = a;
    }

    public void execute(UUID id) {
        var view = repository
                .findActiveViewByPatientAccount(id)
                .orElseThrow(RelationshipConflictException::new);
        var relationship = repository
                .findById(view.relationshipId())
                .orElseThrow(RelationshipNotFoundException::new);
        relationship.end(id, clock.instant());
        repository.save(relationship);
        archivePrescriptions.onRelationshipEnded(relationship.getNutritionistId(), relationship.getPatientId());
    }
}

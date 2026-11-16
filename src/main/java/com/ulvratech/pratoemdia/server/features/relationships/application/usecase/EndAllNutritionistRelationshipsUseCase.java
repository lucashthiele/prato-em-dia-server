package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.RelationshipEndedHandler;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndAllNutritionistRelationships;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class EndAllNutritionistRelationshipsUseCase implements EndAllNutritionistRelationships {
    private final RelationshipRepository repository;
    private final Clock clock;
    private final RelationshipEndedHandler archivePrescriptions;

    public EndAllNutritionistRelationshipsUseCase(
            RelationshipRepository r, Clock c, RelationshipEndedHandler a
    ) {
        repository = r;
        clock = c;
        archivePrescriptions = a;
    }

    public int execute(UUID actorAccountId, UUID nutritionistAccountId) {
        var now = clock.instant();
        var active = repository.findAllActiveByNutritionistAccount(nutritionistAccountId);
        for (var relationship : active) {
            relationship.end(actorAccountId, now);
            repository.save(relationship);
            archivePrescriptions.onRelationshipEnded(relationship.getNutritionistId(), relationship.getPatientId());
        }
        return active.size();
    }
}

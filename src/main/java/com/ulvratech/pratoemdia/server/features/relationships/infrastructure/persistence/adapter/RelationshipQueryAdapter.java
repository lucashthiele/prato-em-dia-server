package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of relationship data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class RelationshipQueryAdapter {
    private final RelationshipJpaRepository relationships;

    RelationshipQueryAdapter(RelationshipJpaRepository relationships) {
        this.relationships = relationships;
    }

    public boolean hasActiveRelationship(UUID nutritionistId, UUID patientId) {
        return relationships.findByNutritionistIdAndPatientIdAndStatus(
                nutritionistId, patientId, RelationshipStatus.ACTIVE
        ).isPresent();
    }

    public boolean hasActiveRelationshipForNutritionistAccount(UUID accountId, UUID patientId) {
        return relationships.findActiveViewByNutritionist(accountId, patientId).isPresent();
    }
}

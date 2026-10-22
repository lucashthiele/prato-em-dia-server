package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of relationship data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class RelationshipQueryAdapter {
    public record PatientAccessCounts(long active, long pending, long failed) {
    }

    private final RelationshipJpaRepository relationships;

    RelationshipQueryAdapter(RelationshipJpaRepository relationships) {
        this.relationships = relationships;
    }

    public PatientAccessCounts patientAccessCounts(UUID nutritionistAccountId) {
        long pending = 0;
        long failed = 0;
        long active = 0;
        for (var row : relationships.countActiveByNutritionistAccountAndEmailStatus(nutritionistAccountId)) {
            long count = (Long) row[1];
            active += count;
            if (row[0] == RelationshipEmailStatus.PENDING)
                pending = count;
            else if (row[0] == RelationshipEmailStatus.FAILED)
                failed = count;
        }
        return new PatientAccessCounts(active, pending, failed);
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

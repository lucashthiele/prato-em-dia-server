package com.ulvratech.pratoemdia.server.features.relationships.domain.port.out;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipFilter;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface RelationshipRepository {
    ProfessionalPatientRelationship save(ProfessionalPatientRelationship relationship);

    Optional<ProfessionalPatientRelationship> findById(UUID id);

    Optional<ProfessionalPatientRelationship> findActiveByNutritionistAndPatient(
            UUID nutritionistId, UUID patientId
    );

    Optional<ProfessionalPatientRelationship> findActiveByPatient(UUID patientId);

    Optional<RelationshipView> findActiveViewByNutritionistAccountAndPatient(
            UUID accountId, UUID patientId
    );

    Optional<RelationshipView> findActiveViewByPatientAccount(UUID accountId);

    Optional<RelationshipView> findViewById(UUID relationshipId);

    PageResult<RelationshipView> findActiveByNutritionistAccount(
            UUID accountId, RelationshipFilter filter, PageRequest page
    );

    Optional<UUID> findNutritionistProfileIdByAccount(UUID accountId);

    Optional<UUID> findPatientProfileIdByAccount(UUID accountId);
}

package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.model.RelationshipModel;
import org.springframework.stereotype.Component;

@Component
public class RelationshipMapper {
  public RelationshipModel toModel(ProfessionalPatientRelationship r) {
    return RelationshipModel.builder()
        .id(r.getId())
        .nutritionistId(r.getNutritionistId())
        .patientId(r.getPatientId())
        .status(r.getStatus())
        .endedAt(r.getEndedAt())
        .endedByUserAccountId(r.getEndedByUserAccountId())
        .emailDeliveryStatus(r.getEmailDeliveryStatus())
        .emailAttemptCount(r.getEmailAttemptCount())
        .emailLastAttemptAt(r.getEmailLastAttemptAt())
        .createdAt(r.getCreatedAt())
        .updatedAt(r.getUpdatedAt())
        .version(r.getVersion())
        .build();
  }

  public ProfessionalPatientRelationship toDomain(RelationshipModel r) {
    return new ProfessionalPatientRelationship(
        r.getId(),
        r.getNutritionistId(),
        r.getPatientId(),
        r.getStatus(),
        r.getEndedAt(),
        r.getEndedByUserAccountId(),
        r.getEmailDeliveryStatus(),
        r.getEmailAttemptCount(),
        r.getEmailLastAttemptAt(),
        r.getCreatedAt(),
        r.getUpdatedAt(),
        r.getVersion());
  }
}

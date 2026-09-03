package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;

import java.time.Instant;
import java.util.UUID;

public record RelationshipOutput(
    UUID relationshipId,
    UUID patientId,
    String patientFullName,
    String patientEmail,
    UUID nutritionistId,
    String nutritionistFullName,
    String relationshipStatus,
    String emailDeliveryStatus,
    Instant createdAt) {
    public static RelationshipOutput from(RelationshipView v) {
        return new RelationshipOutput(
            v.relationshipId(),
            v.patientId(),
            v.patientFullName(),
            v.patientEmail(),
            v.nutritionistId(),
            v.nutritionistFullName(),
            v.status().name(),
            v.emailDeliveryStatus().name(),
            v.createdAt());
    }
}

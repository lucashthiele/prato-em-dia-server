package com.ulvratech.pratoemdia.server.features.relationships.domain.entity;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import java.time.Instant;
import java.util.UUID;

public record RelationshipView(
    UUID relationshipId,
    UUID patientId,
    UUID patientUserAccountId,
    String patientFullName,
    String patientEmail,
    UUID nutritionistId,
    UUID nutritionistUserAccountId,
    String nutritionistFullName,
    RelationshipStatus status,
    RelationshipEmailStatus emailDeliveryStatus,
    Instant createdAt) {}

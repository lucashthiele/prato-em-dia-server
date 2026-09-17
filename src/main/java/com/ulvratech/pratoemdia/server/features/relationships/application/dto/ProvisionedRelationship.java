package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;

public record ProvisionedRelationship(
        RelationshipView view, boolean created, String initialPassword) {
}

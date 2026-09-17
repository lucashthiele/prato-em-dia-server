package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import java.util.UUID;

public record RelationshipQueryInput(UUID accountId, UUID patientId) {}

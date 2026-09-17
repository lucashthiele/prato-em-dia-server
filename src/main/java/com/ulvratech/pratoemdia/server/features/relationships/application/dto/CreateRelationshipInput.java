package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import java.util.UUID;

public record CreateRelationshipInput(UUID nutritionistAccountId, String email, String fullName) {}

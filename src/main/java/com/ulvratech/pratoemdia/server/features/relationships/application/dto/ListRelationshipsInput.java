package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import java.util.UUID;

public record ListRelationshipsInput(UUID accountId, int page, int size) {
}

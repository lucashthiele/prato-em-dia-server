package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;

import java.util.UUID;

public record ListRelationshipsInput(
        UUID accountId,
        int page,
        int size,
        String q,
        RelationshipEmailStatus emailDeliveryStatus,
        Boolean hasOpenDraft,
        String sort) {
    public ListRelationshipsInput(UUID accountId, int page, int size) {
        this(accountId, page, size, null, null, null, null);
    }
}

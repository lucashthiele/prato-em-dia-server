package com.ulvratech.pratoemdia.server.features.consent.domain.entity;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;

import java.time.Instant;
import java.util.UUID;

/** Immutable proof that an account accepted one version of one legal document. */
public record ConsentRecord(
        UUID id, UUID userAccountId, ConsentDocumentType documentType, String version, Instant acceptedAt) {
}

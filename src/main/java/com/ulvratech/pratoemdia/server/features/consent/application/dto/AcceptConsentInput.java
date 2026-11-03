package com.ulvratech.pratoemdia.server.features.consent.application.dto;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;

import java.util.UUID;

public record AcceptConsentInput(UUID userAccountId, ConsentDocumentType documentType, String version) {
}

package com.ulvratech.pratoemdia.server.features.consent.application.dto;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;

import java.time.Instant;
import java.util.List;

public record ConsentStatusOutput(boolean upToDate, List<DocumentStatus> documents) {
    public record DocumentStatus(
            ConsentDocumentType documentType, String currentVersion, boolean accepted, Instant acceptedAt) {
    }
}

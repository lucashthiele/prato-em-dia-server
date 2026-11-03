package com.ulvratech.pratoemdia.server.features.consent.presentation.web.response;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.ConsentStatusOutput;

import java.time.Instant;
import java.util.List;

public record ConsentStatusResponse(boolean upToDate, List<Document> documents) {
    public record Document(String documentType, String currentVersion, boolean accepted, Instant acceptedAt) {
    }

    public static ConsentStatusResponse from(ConsentStatusOutput output) {
        return new ConsentStatusResponse(
                output.upToDate(),
                output.documents().stream().map(
                        d -> new Document(d.documentType().name(), d.currentVersion(), d.accepted(), d.acceptedAt())
                ).toList()
        );
    }
}

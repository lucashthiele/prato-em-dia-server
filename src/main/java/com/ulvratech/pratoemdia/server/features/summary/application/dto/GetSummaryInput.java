package com.ulvratech.pratoemdia.server.features.summary.application.dto;

import java.util.UUID;

public record GetSummaryInput(UUID accountId, int draftLimit) {
}

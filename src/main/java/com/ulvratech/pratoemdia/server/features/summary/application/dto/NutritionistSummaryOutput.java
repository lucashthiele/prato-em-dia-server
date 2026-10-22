package com.ulvratech.pratoemdia.server.features.summary.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NutritionistSummaryOutput(Counts counts, List<DraftToContinue> drafts) {
    /** pendingAccess is the patient not having signed up yet; only failedAccess is a delivery problem. */
    public record Counts(long activePatients, long pendingAccess, long failedAccess, long openDrafts) {
    }

    public record DraftToContinue(
            UUID patientId,
            String patientFullName,
            UUID prescriptionId,
            UUID versionId,
            String title,
            int versionNumber,
            Instant updatedAt) {
    }
}

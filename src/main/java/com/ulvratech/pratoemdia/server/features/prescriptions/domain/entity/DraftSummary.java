package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record DraftSummary(
        UUID patientId,
        String patientFullName,
        UUID prescriptionId,
        UUID versionId,
        String title,
        int versionNumber,
        Instant updatedAt) {
}

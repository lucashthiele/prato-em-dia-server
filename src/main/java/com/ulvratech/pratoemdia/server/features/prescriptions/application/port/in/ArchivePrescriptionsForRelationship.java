package com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in;

import java.util.UUID;

/**
 * Entry point for the end of a nutritionist-patient relationship: archives the pair's prescription.
 * Runs in the caller's transaction. Weekly plans and tracking records are not touched.
 */
public interface ArchivePrescriptionsForRelationship {
    void execute(UUID nutritionistId, UUID patientId);
}

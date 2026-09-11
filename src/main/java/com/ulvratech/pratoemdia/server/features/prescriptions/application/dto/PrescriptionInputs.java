package com.ulvratech.pratoemdia.server.features.prescriptions.application.dto;

import java.util.UUID;

public final class PrescriptionInputs {
    private PrescriptionInputs() { }

    public record Actor(UUID userAccountId, String role) { }
    public record PatientQuery(Actor actor, UUID patientId) { }
    public record PrescriptionQuery(Actor actor, UUID prescriptionId) { }
    public record VersionQuery(Actor actor, UUID versionId) { }
    public record Create(Actor actor, UUID patientId, PrescriptionContentInput content) { }
    public record UpdateVersion(Actor actor, UUID versionId, PrescriptionContentInput content) { }
}

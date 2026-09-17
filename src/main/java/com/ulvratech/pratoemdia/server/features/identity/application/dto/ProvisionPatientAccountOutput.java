package com.ulvratech.pratoemdia.server.features.identity.application.dto;

import java.util.UUID;

public record ProvisionPatientAccountOutput(
    UUID userAccountId,
    String email,
    boolean created,
    boolean passwordChangeRequired,
    String initialPassword) {}

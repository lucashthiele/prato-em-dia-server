package com.ulvratech.pratoemdia.server.features.admin.application.dto;

import java.util.UUID;

/** The temporary password is never part of this output: it only exists inside the access email. */
public record CreateNutritionistOutput(
        UUID userAccountId, String email, String fullName, String registrationNumber, String accessEmailStatus) {
}

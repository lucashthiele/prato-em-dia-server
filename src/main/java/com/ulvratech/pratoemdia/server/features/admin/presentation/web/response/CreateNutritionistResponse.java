package com.ulvratech.pratoemdia.server.features.admin.presentation.web.response;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.CreateNutritionistOutput;

import java.util.UUID;

/** Never carries the temporary password; it only travels in the access email. */
public record CreateNutritionistResponse(
        UUID id, String email, String fullName, String registrationNumber, String accessEmailStatus) {
    public static CreateNutritionistResponse from(CreateNutritionistOutput output) {
        return new CreateNutritionistResponse(
                output.userAccountId(), output.email(), output.fullName(), output.registrationNumber(),
                output.accessEmailStatus()
        );
    }
}

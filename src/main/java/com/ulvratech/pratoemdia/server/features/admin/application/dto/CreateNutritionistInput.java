package com.ulvratech.pratoemdia.server.features.admin.application.dto;

import java.util.UUID;

public record CreateNutritionistInput(UUID actorAccountId, String email, String fullName, String registrationNumber) {
}

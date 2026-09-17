package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import java.util.UUID;

public record UpdateFoodInput(
        UUID foodId,
        UUID actorUserId,
        boolean admin,
        String name,
        String category,
        String defaultUnit,
        String notes,
        String preparationMethod,
        boolean active) {
}

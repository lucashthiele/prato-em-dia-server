package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import java.util.List;
import java.util.UUID;

public record CreateFoodInput(
    UUID actorUserId,
    String name,
    String category,
    String defaultUnit,
    String notes,
    String preparationMethod,
    List<CreateFoodPortionInput> portions
) {
}

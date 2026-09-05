package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;

import java.util.List;
import java.util.UUID;

public record CreateFoodInput(
    UUID actorUserId,
    FoodOrigin origin,
    String name,
    String category,
    String defaultUnit,
    String notes,
    String preparationMethod,
    List<CreateFoodPortionInput> portions
) {
}

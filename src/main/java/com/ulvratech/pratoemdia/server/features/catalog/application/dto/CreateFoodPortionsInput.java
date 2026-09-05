package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import java.util.List;
import java.util.UUID;

public record CreateFoodPortionsInput(
    UUID foodId,
    List<CreateFoodPortionInput> portions
) {
}

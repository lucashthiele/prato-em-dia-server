package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import org.springframework.stereotype.Component;

@Component
public class FoodMapper {
    public FoodModel toModel(Food food) {
        var model = FoodModel.builder()
            .id(food.getId())
            .name(food.getName())
            .brand(food.getBrand())
            .category(food.getCategory())
            .defaultUnit(food.getDefaultUnit())
            .notes(food.getNotes())
            .preparationMethod(food.getPreparationMethod())
            .active(food.isActive())
            .createdByUserId(food.getCreatedByUserId())
            .origin(food.getOrigin())
            .source(food.getSource())
            .sourceExternalId(food.getSourceExternalId())
            .sourceDescription(food.getSourceDescription())
            .sourceVersion(food.getSourceVersion())
            .normalizedName(food.getNormalizedName())
            .createdAt(food.getCreatedAt())
            .updatedAt(food.getUpdatedAt())
            .version(food.getVersion())
            .build();
        return model;
    }

    public Food toDomain(FoodModel food) {
        return new Food(
            food.getId(), food.getName(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getOrigin(), food.getCreatedAt(), food.getUpdatedAt(),
            food.getVersion(), food.getSource(), food.getSourceExternalId(),
            food.getSourceDescription(), food.getSourceVersion(), food.getNormalizedName(),
            food.getBrand()
        );
    }
}

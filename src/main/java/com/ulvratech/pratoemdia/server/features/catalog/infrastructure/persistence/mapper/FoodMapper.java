package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import org.springframework.stereotype.Component;

@Component
public class FoodMapper {
    public FoodModel toModel(Food food) {
        return FoodModel.builder()
            .id(food.getId())
            .name(food.getName())
            .category(food.getCategory())
            .defaultUnit(food.getDefaultUnit())
            .notes(food.getNotes())
            .preparationMethod(food.getPreparationMethod())
            .active(food.isActive())
            .createdByUserId(food.getCreatedByUserId())
            .createdAt(food.getCreatedAt())
            .updatedAt(food.getUpdatedAt())
            .version(food.getVersion())
            .build();
    }

    public Food toDomain(FoodModel food) {
        return new Food(
            food.getId(), food.getName(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getCreatedAt(), food.getUpdatedAt(), food.getVersion()
        );
    }
}

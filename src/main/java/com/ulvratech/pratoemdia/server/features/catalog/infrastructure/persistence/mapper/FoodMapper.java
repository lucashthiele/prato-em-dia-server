package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodNutrientModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class FoodMapper {
    public FoodModel toModel(Food food) {
        var model = FoodModel.builder()
            .id(food.getId())
            .name(food.getName())
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
        model.setNutrients(food.getNutrients().entrySet().stream()
            .map(entry -> toNutrientModel(model, entry.getKey(), entry.getValue()))
            .collect(Collectors.toCollection(ArrayList::new)));
        return model;
    }

    public Food toDomain(FoodModel food) {
        return new Food(
            food.getId(), food.getName(), food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive(),
            food.getCreatedByUserId(), food.getOrigin(), food.getCreatedAt(), food.getUpdatedAt(),
            food.getVersion(), food.getSource(), food.getSourceExternalId(),
            food.getSourceDescription(), food.getSourceVersion(), food.getNormalizedName(),
            food.getNutrients().stream().collect(Collectors.toMap(
                FoodNutrientModel::getNutrientCode,
                nutrient -> new NutrientValue(
                    nutrient.getAmount(), nutrient.getUnit(), nutrient.getValueStatus(),
                    nutrient.getRawValue()
                )
            ))
        );
    }

    private FoodNutrientModel toNutrientModel(
        FoodModel food, String code, NutrientValue value
    ) {
        return FoodNutrientModel.builder()
            .id(CatalogIds.foodNutrient(food.getId(), code))
            .food(food)
            .nutrientCode(code)
            .amount(value.amount())
            .unit(value.unit())
            .valueStatus(value.status())
            .rawValue(value.rawValue())
            .build();
    }
}

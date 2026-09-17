package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionNutrientModel;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FoodPortionMapper {
  public FoodPortionModel toModel(FoodPortion portion) {
    var model =
        FoodPortionModel.builder()
            .id(portion.getId())
            .foodId(portion.getFoodId())
            .label(portion.getLabel())
            .quantity(portion.getQuantity())
            .unit(portion.getUnit())
            .gramsEquivalent(portion.getGramsEquivalent())
            .sourceType(portion.getSourceType())
            .rawLabel(portion.getRawLabel())
            .active(portion.isActive())
            .createdAt(portion.getCreatedAt())
            .updatedAt(portion.getUpdatedAt())
            .build();
    model.setNutrients(
        portion.getNutrients().entrySet().stream()
            .map(entry -> toNutrientModel(model, entry.getKey(), entry.getValue()))
            .collect(Collectors.toCollection(ArrayList::new)));
    return model;
  }

  public FoodPortion toDomain(FoodPortionModel portion) {
    return new FoodPortion(
        portion.getId(),
        portion.getFoodId(),
        portion.getLabel(),
        portion.getQuantity(),
        portion.getUnit(),
        portion.getGramsEquivalent(),
        portion.isActive(),
        portion.getCreatedAt(),
        portion.getUpdatedAt(),
        portion.getSourceType(),
        portion.getRawLabel(),
        portion.getNutrients().stream()
            .collect(
                Collectors.toMap(
                    FoodPortionNutrientModel::getNutrientCode,
                    nutrient ->
                        new NutrientValue(
                            nutrient.getAmount(),
                            nutrient.getUnit(),
                            nutrient.getValueStatus(),
                            nutrient.getRawValue()))));
  }

  private FoodPortionNutrientModel toNutrientModel(
      FoodPortionModel portion, String code, NutrientValue value) {
    return FoodPortionNutrientModel.builder()
        .id(CatalogIds.foodPortionNutrient(portion.getId(), code))
        .portion(portion)
        .nutrientCode(code)
        .amount(value.amount())
        .unit(value.unit())
        .valueStatus(value.status())
        .rawValue(value.rawValue())
        .build();
  }
}

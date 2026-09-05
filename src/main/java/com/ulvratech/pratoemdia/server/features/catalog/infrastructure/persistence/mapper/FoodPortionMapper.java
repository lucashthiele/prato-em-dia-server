package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodPortionModel;
import org.springframework.stereotype.Component;

@Component
public class FoodPortionMapper {
    public FoodPortionModel toModel(FoodPortion portion) {
        return FoodPortionModel.builder()
            .id(portion.getId())
            .foodId(portion.getFoodId())
            .label(portion.getLabel())
            .quantity(portion.getQuantity())
            .unit(portion.getUnit())
            .gramsEquivalent(portion.getGramsEquivalent())
            .active(portion.isActive())
            .createdAt(portion.getCreatedAt())
            .updatedAt(portion.getUpdatedAt())
            .build();
    }

    public FoodPortion toDomain(FoodPortionModel portion) {
        return new FoodPortion(
            portion.getId(), portion.getFoodId(), portion.getLabel(), portion.getQuantity(),
            portion.getUnit(), portion.getGramsEquivalent(), portion.isActive(),
            portion.getCreatedAt(), portion.getUpdatedAt()
        );
    }
}

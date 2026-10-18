package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper.FoodMapper;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper.FoodPortionMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of catalog data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class CatalogQueryAdapter {
    public record Nutrient(String code, BigDecimal amount, String unit) {
    }

    public record Portion(UUID id, BigDecimal quantity, String unit, BigDecimal gramsEquivalent,
            List<Nutrient> nutrients) {
    }

    private final FoodMapper foodMapper;
    private final FoodPortionMapper portionMapper;
    private final FoodJpaRepository foods;
    private final FoodPortionJpaRepository portions;

    CatalogQueryAdapter(
            FoodMapper foodMapper, FoodPortionMapper portionMapper, FoodJpaRepository foods,
            FoodPortionJpaRepository portions
    ) {
        this.foodMapper = foodMapper;
        this.portionMapper = portionMapper;
        this.foods = foods;
        this.portions = portions;
    }

    public Optional<Food> findActiveFood(UUID foodId) {
        return foods.findByIdAndActiveTrue(foodId).map(foodMapper::toDomain);
    }

    public Optional<FoodPortion> findActivePortion(UUID foodId, UUID portionId) {
        return portions.findByIdAndFoodIdAndActiveTrue(portionId, foodId).map(portionMapper::toDomain);
    }

    public boolean isActiveFood(UUID foodId) {
        return foods.existsByIdAndActiveTrue(foodId);
    }

    public boolean portionBelongsToFood(UUID portionId, UUID foodId) {
        return portions.existsByIdAndFoodIdAndActiveTrue(portionId, foodId);
    }

    public Optional<Portion> activePortion(UUID foodId, UUID portionId) {
        if (!foods.existsByIdAndActiveTrue(foodId))
            return Optional.empty();
        return portions.findByIdAndFoodIdAndActiveTrue(portionId, foodId)
                .map(
                        portion -> new Portion(
                                portion.getId(), portion.getQuantity(), portion.getUnit(),
                                portion.getGramsEquivalent(),
                                portion.getNutrients().stream()
                                        .map(n -> new Nutrient(n.getNutrientCode(), n.getAmount(), n.getUnit()))
                                        .toList()
                        )
                );
    }
}

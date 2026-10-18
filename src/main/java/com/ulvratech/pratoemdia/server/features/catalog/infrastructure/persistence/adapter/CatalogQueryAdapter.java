package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

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

    private final FoodJpaRepository foods;
    private final FoodPortionJpaRepository portions;

    CatalogQueryAdapter(FoodJpaRepository foods, FoodPortionJpaRepository portions) {
        this.foods = foods;
        this.portions = portions;
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

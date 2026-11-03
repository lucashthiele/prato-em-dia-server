package com.ulvratech.pratoemdia.server.features.prescriptions.application.service;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescriptionVersion;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Foods and portions referenced by prescription versions, loaded with two batch queries. */
public record SummaryCatalog(Map<UUID, Food> foods, Map<UUID, FoodPortion> portions) {
    public static SummaryCatalog load(
            PrescriptionReferences references, List<PrescriptionVersion> versions
    ) {
        Set<UUID> foodIds = new HashSet<>();
        Set<UUID> portionIds = new HashSet<>();
        for (var version : versions)
            for (var meal : version.getMeals())
                for (var item : meal.items()) {
                    foodIds.add(item.foodId());
                    if (item.foodPortionId() != null)
                        portionIds.add(item.foodPortionId());
                    for (var substitution : item.substitutions()) {
                        foodIds.add(substitution.foodId());
                        if (substitution.foodPortionId() != null)
                            portionIds.add(substitution.foodPortionId());
                    }
                }
        return new SummaryCatalog(references.foods(foodIds), references.portions(portionIds));
    }

    public String foodName(UUID foodId) {
        return Optional.ofNullable(foods.get(foodId)).map(Food::getName).orElse(null);
    }

    public FoodPortion portion(UUID portionId) {
        return portionId == null ? null : portions.get(portionId);
    }
}

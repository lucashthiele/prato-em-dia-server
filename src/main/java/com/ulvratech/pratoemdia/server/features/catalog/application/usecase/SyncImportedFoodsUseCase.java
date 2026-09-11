package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.*;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.FoodNameNormalizer;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SyncImportedFoodsUseCase implements SyncImportedFoods {
    private final FoodRepository foods;
    private final FoodPortionRepository portions;

    public SyncImportedFoodsUseCase(FoodRepository foods, FoodPortionRepository portions) {
        this.foods = foods;
        this.portions = portions;
    }

    @Override
    public SyncImportedFoodsOutput execute(SyncImportedFoodsInput input) {
        var externalIds = input.foods().stream().map(ImportedFoodInput::externalId).toList();
        var existing = foods.findBySourceAndExternalIds(input.source(), externalIds).stream()
            .collect(Collectors.toMap(Food::getSourceExternalId, Function.identity()));
        var existingPortions = portions.findImportedByFoodIds(
            existing.values().stream().map(Food::getId).toList()
        ).stream().collect(Collectors.groupingBy(FoodPortion::getFoodId));

        var changedFoods = new ArrayList<Food>();
        var changedPortions = new ArrayList<FoodPortion>();
        var changedFoodIds = new ArrayList<UUID>();
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (var imported : input.foods()) {
            String normalized = FoodNameNormalizer.normalize(imported.description());
            var food = existing.get(imported.externalId());
            UUID foodId = CatalogIds.importedFood(input.source(), imported.externalId());
            var desiredPortions = toPortions(foodId, imported.portions());

            if (food == null) {
                inserted++;
                food = Food.imported(
                    foodId, imported.description(), imported.category(), input.source(),
                    imported.externalId(), imported.description(), input.sourceVersion(),
                    normalized, imported.brand()
                );
                changedFoods.add(food);
                changedFoodIds.add(foodId);
                changedPortions.addAll(desiredPortions);
            } else {
                if (!food.getId().equals(foodId)) {
                    throw new IllegalStateException(
                        "Existing imported food has a non-deterministic UUID: "
                            + input.source() + "/" + imported.externalId()
                    );
                }
                boolean metadataMatches = food.hasImportedContent(
                    imported.description(), imported.category(), imported.description(),
                    input.sourceVersion(), normalized, imported.brand()
                );
                boolean portionsMatch = samePortions(
                    existingPortions.getOrDefault(foodId, List.of()), desiredPortions
                );
                if (metadataMatches && portionsMatch) {
                    skipped++;
                    continue;
                }
                updated++;
                food.updateImported(
                    imported.description(), imported.category(), imported.description(),
                    input.sourceVersion(), normalized, imported.brand()
                );
                changedFoods.add(food);
                changedFoodIds.add(foodId);
                changedPortions.addAll(desiredPortions);
            }
        }

        if (!input.dryRun()) {
            foods.saveAll(changedFoods);
            portions.replaceImportedForFoods(changedFoodIds, changedPortions);
        }
        return new SyncImportedFoodsOutput(inserted, updated, skipped);
    }

    private List<FoodPortion> toPortions(
        UUID foodId, List<ImportedFoodPortionInput> importedPortions
    ) {
        return importedPortions.stream().map(portion -> FoodPortion.imported(
            CatalogIds.importedFoodPortion(
                foodId, portion.type(), portion.label(), portion.quantity(), portion.unit()
            ),
            foodId, portion.type(), portion.label(), portion.rawLabel(), portion.quantity(),
            portion.unit(), portion.nutrients()
        )).toList();
    }

    private boolean samePortions(List<FoodPortion> existing, List<FoodPortion> desired) {
        if (existing.size() != desired.size()) return false;
        var byId = existing.stream().collect(Collectors.toMap(FoodPortion::getId, Function.identity()));
        return desired.stream().allMatch(candidate -> {
            var current = byId.get(candidate.getId());
            return current != null
                && Objects.equals(current.getLabel(), candidate.getLabel())
                && Objects.equals(current.getQuantity(), candidate.getQuantity())
                && Objects.equals(current.getUnit(), candidate.getUnit())
                && Objects.equals(current.getGramsEquivalent(), candidate.getGramsEquivalent())
                && Objects.equals(current.getSourceType(), candidate.getSourceType())
                && Objects.equals(current.getRawLabel(), candidate.getRawLabel())
                && Objects.equals(current.getNutrients(), candidate.getNutrients())
                && current.isActive();
        });
    }
}

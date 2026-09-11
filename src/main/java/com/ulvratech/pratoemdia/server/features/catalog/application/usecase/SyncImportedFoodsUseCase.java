package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.FoodNameNormalizer;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SyncImportedFoodsUseCase implements SyncImportedFoods {
    private final FoodRepository foods;

    public SyncImportedFoodsUseCase(FoodRepository foods) {
        this.foods = foods;
    }

    @Override
    public SyncImportedFoodsOutput execute(SyncImportedFoodsInput input) {
        var externalIds = input.foods().stream().map(ImportedFoodInput::externalId).toList();
        var existing = foods.findBySourceAndExternalIds(input.source(), externalIds).stream()
            .collect(Collectors.toMap(Food::getSourceExternalId, Function.identity()));
        var changed = new ArrayList<Food>();
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (var imported : input.foods()) {
            String normalized = FoodNameNormalizer.normalize(imported.description());
            var food = existing.get(imported.externalId());
            if (food == null) {
                inserted++;
                changed.add(Food.imported(
                    CatalogIds.importedFood(input.source(), imported.externalId()),
                    imported.description(), imported.category(), input.source(), imported.externalId(),
                    imported.description(), input.sourceVersion(), normalized, imported.nutrients()
                ));
            } else if (food.hasImportedContent(
                imported.description(), imported.category(), imported.description(),
                input.sourceVersion(), normalized, imported.nutrients()
            )) {
                skipped++;
            } else {
                if (!food.getId().equals(CatalogIds.importedFood(input.source(), imported.externalId()))) {
                    throw new IllegalStateException(
                        "Existing imported food has a non-deterministic UUID: "
                            + input.source() + "/" + imported.externalId()
                    );
                }
                updated++;
                food.updateImported(
                    imported.description(), imported.category(), imported.description(),
                    input.sourceVersion(), normalized, imported.nutrients()
                );
                changed.add(food);
            }
        }

        if (!input.dryRun()) foods.saveAll(changed);
        return new SyncImportedFoodsOutput(inserted, updated, skipped);
    }
}

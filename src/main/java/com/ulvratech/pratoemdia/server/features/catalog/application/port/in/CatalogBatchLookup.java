package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Batch lookup of foods and portions for other features. It ignores the active flag, because
 * historical records (prescriptions, plans) may reference foods or portions that were deactivated.
 */
public interface CatalogBatchLookup {
    Map<UUID, Food> foodsByIds(Collection<UUID> foodIds);

    Map<UUID, FoodPortion> portionsByIds(Collection<UUID> portionIds);
}

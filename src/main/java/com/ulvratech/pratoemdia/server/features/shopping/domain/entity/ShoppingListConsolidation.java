package com.ulvratech.pratoemdia.server.features.shopping.domain.entity;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Sums planned lines by food, portion and unit, without breaking dishes into ingredients. */
public final class ShoppingListConsolidation {
    private record Key(java.util.UUID foodId, java.util.UUID portionId, String unit) {
    }

    private ShoppingListConsolidation() {
    }

    /** Items come in the order each key first appears. */
    public static List<ShoppingListItem> consolidate(List<PlannedLine> lines) {
        var totals = new LinkedHashMap<Key, BigDecimal>();
        for (var line : lines) {
            Objects.requireNonNull(line.foodId(), "foodId is required");
            totals.merge(new Key(line.foodId(), line.foodPortionId(), line.unit()), line.quantity(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(
                        entry -> new ShoppingListItem(
                                entry.getKey().foodId(), entry.getKey().portionId(), entry.getKey().unit(),
                                entry.getValue()
                        )
                ).toList();
    }
}

package com.ulvratech.pratoemdia.server.features.shopping.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.PlannedLine;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingListConsolidation;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShoppingListConsolidationTest {
    final UUID rice = UUID.randomUUID();
    final UUID beans = UUID.randomUUID();
    final UUID ricePortion = UUID.randomUUID();
    final UUID riceCup = UUID.randomUUID();

    PlannedLine line(UUID food, UUID portion, String unit, String quantity) {
        return new PlannedLine(food, portion, unit, new BigDecimal(quantity));
    }

    @Test
    void sumsLinesOfTheSameFoodPortionAndUnit() {
        var items = ShoppingListConsolidation.consolidate(
                List.of(
                        line(rice, ricePortion, "g", "100"), line(beans, null, "g", "50"),
                        line(rice, ricePortion, "g", "150.5")
                )
        );

        assertEquals(2, items.size());
        assertEquals(rice, items.get(0).foodId());
        assertEquals(0, new BigDecimal("250.5").compareTo(items.get(0).quantity()));
        assertEquals(beans, items.get(1).foodId());
        assertNull(items.get(1).foodPortionId());
    }

    @Test
    void keepsDifferentPortionsAndUnitsApart() {
        var items = ShoppingListConsolidation.consolidate(
                List.of(
                        line(rice, ricePortion, "g", "100"), line(rice, riceCup, "g", "100"),
                        line(rice, ricePortion, "ml", "100")
                )
        );

        assertEquals(3, items.size());
    }

    @Test
    void ordersItemsByFirstAppearance() {
        var items = ShoppingListConsolidation.consolidate(
                List.of(
                        line(beans, null, "g", "1"), line(rice, null, "g", "1"), line(beans, null, "g", "1")
                )
        );

        assertEquals(List.of(beans, rice), items.stream().map(i -> i.foodId()).toList());
    }

    @Test
    void emptyPlanGivesAnEmptyList() {
        assertTrue(ShoppingListConsolidation.consolidate(List.of()).isEmpty());
    }
}

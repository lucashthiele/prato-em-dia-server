package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FoodPortionScalingTest {
    FoodPortion portion(String quantity, String grams, Map<String, NutrientValue> nutrients) {
        return new FoodPortion(
                UUID.randomUUID(), UUID.randomUUID(), "portion", new BigDecimal(quantity), "g",
                grams == null ? null : new BigDecimal(grams), true, null, null, null, null, nutrients
        );
    }

    NutrientValue value(String amount) {
        return new NutrientValue(new BigDecimal(amount), "g", NutrientValueStatus.VALUE, null);
    }

    @Test
    void scalesANutrientByQuantityOverPortionQuantity() {
        var portion = portion("100", "100", Map.of("protein", value("10")));

        assertEquals(
                0,
                new BigDecimal("25").compareTo(portion.scaledNutrient("protein", new BigDecimal("250")).orElseThrow())
        );
    }

    @Test
    void keepsTwelveDigitsOfPrecisionInTheFactor() {
        var portion = portion("3", null, Map.of("energy_kcal", value("100")));

        assertEquals(new BigDecimal("0.333333333333"), portion.scaleFactor(BigDecimal.ONE));
        assertEquals(
                "33.3333333333", portion.scaledNutrient("energy_kcal", BigDecimal.ONE).orElseThrow().toString()
        );
    }

    @Test
    void absentOrNonNumericNutrientsHaveNoScaledValue() {
        var nonNumeric = new NutrientValue(null, "g", NutrientValueStatus.TRACE, "tr");
        var portion = portion("100", null, Map.of("fiber", nonNumeric));

        assertTrue(portion.scaledNutrient("protein", BigDecimal.TEN).isEmpty());
        assertTrue(portion.scaledNutrient("fiber", BigDecimal.TEN).isEmpty());
    }

    @Test
    void scalesTheGramEquivalentOnlyWhenKnown() {
        assertEquals(
                0,
                new BigDecimal("60")
                        .compareTo(portion("2", "30", Map.of()).scaledGrams(new BigDecimal("4")).orElseThrow())
        );
        assertTrue(portion("2", null, Map.of()).scaledGrams(new BigDecimal("4")).isEmpty());
    }
}

package com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;

import java.math.BigDecimal;

public class TacoNutrientValueParser {
    public NutrientValue parse(Object value, String unit) {
        if (value == null) return special(unit, NutrientValueStatus.NOT_AVAILABLE, null);
        if (value instanceof Number number) {
            return numeric(new BigDecimal(number.toString()), unit, number.toString());
        }

        String raw = value.toString();
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.equals("*")) {
            return special(unit, NutrientValueStatus.NOT_AVAILABLE, raw);
        }
        if (trimmed.equalsIgnoreCase("Tr")) {
            return special(unit, NutrientValueStatus.TRACE, raw);
        }
        if (trimmed.equalsIgnoreCase("NA")) {
            return special(unit, NutrientValueStatus.NOT_APPLICABLE, raw);
        }
        // Known typo in TACO 4th edition, food 373 / pyridoxine: ",0,02".
        if (trimmed.equals(",0,02")) {
            return numeric(new BigDecimal("0.02"), unit, raw);
        }
        try {
            return numeric(new BigDecimal(trimmed.replace(',', '.')), unit, raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("unsupported nutrient value: " + raw);
        }
    }

    private NutrientValue numeric(BigDecimal amount, String unit, String raw) {
        return new NutrientValue(amount.stripTrailingZeros(), unit, NutrientValueStatus.VALUE, raw);
    }

    private NutrientValue special(
        String unit, NutrientValueStatus status, String raw
    ) {
        return new NutrientValue(null, unit, status, raw);
    }
}

package com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TacoNutrientValueParserTest {
    private final TacoNutrientValueParser parser = new TacoNutrientValueParser();

    @Test
    void distinguishesZeroMissingTraceAndNotApplicable() {
        assertEquals(BigDecimal.ZERO, parser.parse("0", "g").amount());
        assertEquals(NutrientValueStatus.NOT_AVAILABLE, parser.parse(null, "g").status());
        assertEquals(NutrientValueStatus.NOT_AVAILABLE, parser.parse("*", "g").status());
        assertEquals(NutrientValueStatus.TRACE, parser.parse("Tr", "g").status());
        assertEquals(NutrientValueStatus.NOT_APPLICABLE, parser.parse("NA", "g").status());
    }

    @Test
    void explicitlyCorrectsKnownTacoTypoAndRejectsUnknownText() {
        var corrected = parser.parse(",0,02", "mg");

        assertEquals(new BigDecimal("0.02"), corrected.amount());
        assertEquals(",0,02", corrected.rawValue());
        assertThrows(IllegalArgumentException.class, () -> parser.parse("unknown", "mg"));
    }
}

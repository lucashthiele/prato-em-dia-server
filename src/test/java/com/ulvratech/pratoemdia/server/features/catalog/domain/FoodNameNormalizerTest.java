package com.ulvratech.pratoemdia.server.features.catalog.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodNameNormalizerTest {
    @Test
    void removesAccentsPunctuationCaseAndRepeatedWhitespace() {
        assertEquals(
                "arroz integral cozido",
                FoodNameNormalizer.normalize(" Arroz,  integral, cozido ")
        );
    }
}

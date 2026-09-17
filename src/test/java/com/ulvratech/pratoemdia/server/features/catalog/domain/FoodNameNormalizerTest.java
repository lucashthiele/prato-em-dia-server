package com.ulvratech.pratoemdia.server.features.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FoodNameNormalizerTest {
  @Test
  void removesAccentsPunctuationCaseAndRepeatedWhitespace() {
    assertEquals(
        "arroz integral cozido", FoodNameNormalizer.normalize(" Arroz,  integral, cozido "));
  }
}

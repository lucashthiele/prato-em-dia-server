package com.ulvratech.pratoemdia.server.features.catalog.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class FoodNameNormalizer {
  private FoodNameNormalizer() {}

  public static String normalize(String value) {
    if (value == null) return null;
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .trim()
        .replaceAll("\\s+", " ");
  }
}

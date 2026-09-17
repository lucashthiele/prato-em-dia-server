package com.ulvratech.pratoemdia.server.features.importer.application.dto;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import java.util.Map;

public record TacoFoodRecord(
    String externalId, String description, String category, Map<String, NutrientValue> nutrients) {
  public TacoFoodRecord {
    nutrients = Map.copyOf(nutrients);
  }
}

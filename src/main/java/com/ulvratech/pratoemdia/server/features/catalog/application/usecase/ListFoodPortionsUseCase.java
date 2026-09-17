package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodPortionOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodQueryInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodNotFoundException;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.ListFoodPortions;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListFoodPortionsUseCase implements ListFoodPortions {
  private final FoodRepository foods;
  private final FoodPortionRepository portions;

  public ListFoodPortionsUseCase(FoodRepository foods, FoodPortionRepository portions) {
    this.foods = foods;
    this.portions = portions;
  }

  @Override
  public List<FoodPortionOutput> execute(FoodQueryInput input) {
    foods.findActiveById(input.foodId()).orElseThrow(FoodNotFoundException::new);
    return portions.findActiveByFoodId(input.foodId()).stream()
        .map(FoodPortionOutput::from)
        .toList();
  }
}

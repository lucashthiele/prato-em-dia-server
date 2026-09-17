package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CreateFood;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CreateFoodUseCase implements CreateFood {
  private final FoodRepository repository;
  private final FoodPortionRepository portions;

  public CreateFoodUseCase(FoodRepository repository, FoodPortionRepository portions) {
    this.repository = repository;
    this.portions = portions;
  }

  @Override
  public FoodOutput execute(CreateFoodInput input) {
    var food =
        Food.create(
            input.name(),
            input.category(),
            input.defaultUnit(),
            input.notes(),
            input.preparationMethod(),
            input.actorUserId(),
            input.origin());
    var savedFood = repository.save(food);
    var foodPortions =
        input.portions().stream()
            .map(
                portion ->
                    FoodPortion.create(
                        savedFood.getId(),
                        portion.label(),
                        portion.quantity(),
                        portion.unit(),
                        portion.gramsEquivalent()))
            .toList();
    portions.saveAll(foodPortions);
    return FoodOutput.from(savedFood);
  }
}

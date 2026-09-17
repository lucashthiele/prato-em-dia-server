package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodPortionOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodNotFoundException;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CreateFoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class CreateFoodPortionUseCase implements CreateFoodPortion {
    private final FoodRepository foods;
    private final FoodPortionRepository portions;

    public CreateFoodPortionUseCase(FoodRepository foods, FoodPortionRepository portions) {
        this.foods = foods;
        this.portions = portions;
    }

    @Override
    public List<FoodPortionOutput> execute(CreateFoodPortionsInput input) {
        foods.findActiveById(input.foodId()).orElseThrow(FoodNotFoundException::new);
        var newPortions = input.portions().stream()
                .map(
                        portion -> FoodPortion.create(
                                input.foodId(), portion.label(), portion.quantity(), portion.unit(),
                                portion.gramsEquivalent()
                        )
                )
                .toList();
        return portions.saveAll(newPortions).stream().map(FoodPortionOutput::from).toList();
    }
}

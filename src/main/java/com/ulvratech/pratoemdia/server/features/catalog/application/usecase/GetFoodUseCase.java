package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodDetailsOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodQueryInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodNotFoundException;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.GetFood;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.springframework.stereotype.Service;

@Service
public class GetFoodUseCase implements GetFood {
    private final FoodRepository repository;
    private final FoodPortionRepository portions;

    public GetFoodUseCase(FoodRepository repository, FoodPortionRepository portions) {
        this.repository = repository;
        this.portions = portions;
    }

    @Override
    public FoodDetailsOutput execute(FoodQueryInput input) {
        var food = repository.findActiveById(input.foodId())
            .orElseThrow(FoodNotFoundException::new);
        return FoodDetailsOutput.from(food, portions.findActiveByFoodId(food.getId()));
    }
}

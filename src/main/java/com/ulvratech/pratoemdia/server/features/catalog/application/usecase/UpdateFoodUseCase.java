package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.UpdateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodNotFoundException;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.UpdateFood;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UpdateFoodUseCase implements UpdateFood {
    private final FoodRepository repository;

    public UpdateFoodUseCase(FoodRepository repository) {
        this.repository = repository;
    }

    @Override
    public FoodOutput execute(UpdateFoodInput input) {
        var food = repository.findById(input.foodId()).orElseThrow(FoodNotFoundException::new);
        if (!input.admin() && !food.getCreatedByUserId().equals(input.actorUserId())) {
            throw new FoodAccessDeniedException();
        }
        food.update(
            input.name(), input.category(), input.defaultUnit(), input.notes(),
            input.preparationMethod(), input.active()
        );
        return FoodOutput.from(repository.save(food));
    }
}

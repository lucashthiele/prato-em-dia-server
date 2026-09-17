package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionsInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateFoodPortionUseCaseTest {
    @Test
    void createsAllPortionsAfterCheckingFoodOnce() {
        var foods = mock(FoodRepository.class);
        var portions = mock(FoodPortionRepository.class);
        var food = Food.create(
                "Arroz", null, "g", null, null, UUID.randomUUID(), FoodOrigin.PATIENT
        );
        when(foods.findActiveById(food.getId())).thenReturn(Optional.of(food));
        when(portions.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CreateFoodPortionUseCase(foods, portions);

        var output = useCase.execute(
                new CreateFoodPortionsInput(
                        food.getId(), List.of(
                                new CreateFoodPortionInput("Colher", new BigDecimal("25"), "g", null),
                                new CreateFoodPortionInput("Xícara", new BigDecimal("160"), "g", null)
                        )
                )
        );

        assertEquals(2, output.size());
        verify(foods, times(1)).findActiveById(food.getId());
        verify(portions).saveAll(anyList());
    }
}

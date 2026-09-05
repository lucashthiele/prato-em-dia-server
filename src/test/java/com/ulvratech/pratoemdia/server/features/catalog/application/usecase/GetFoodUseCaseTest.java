package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodQueryInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetFoodUseCaseTest {
    @Test
    void returnsFoodWithItsActivePortions() {
        var foods = mock(FoodRepository.class);
        var portions = mock(FoodPortionRepository.class);
        var food = Food.create(
            "Arroz", "Cereais", "g", null, null, UUID.randomUUID(), FoodOrigin.IMPORTED
        );
        var portion = FoodPortion.create(
            food.getId(), "Colher", new BigDecimal("25"), "g", new BigDecimal("25")
        );
        when(foods.findActiveById(food.getId())).thenReturn(Optional.of(food));
        when(portions.findActiveByFoodId(food.getId())).thenReturn(List.of(portion));

        var output = new GetFoodUseCase(foods, portions)
            .execute(new FoodQueryInput(food.getId()));

        assertEquals(food.getId(), output.id());
        assertEquals(FoodOrigin.IMPORTED, output.origin());
        assertEquals(1, output.portions().size());
        assertEquals("Colher", output.portions().getFirst().label());
        verify(portions).findActiveByFoodId(food.getId());
    }
}

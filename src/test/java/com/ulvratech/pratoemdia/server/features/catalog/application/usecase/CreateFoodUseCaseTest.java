package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateFoodUseCaseTest {
    @Test
    void createsFoodAndItsPortionsInTheSameUseCase() {
        var foods = mock(FoodRepository.class);
        var portions = mock(FoodPortionRepository.class);
        when(foods.save(any(Food.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(portions.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var useCase = new CreateFoodUseCase(foods, portions);

        var output = useCase.execute(new CreateFoodInput(
            UUID.randomUUID(), "Arroz", "Cereais", "g", null, null,
            List.of(new CreateFoodPortionInput(
                "Colher", new BigDecimal("25"), "g", new BigDecimal("25")
            ))
        ));

        @SuppressWarnings("unchecked")
        var captor = ArgumentCaptor.forClass((Class<List<FoodPortion>>) (Class<?>) List.class);
        verify(portions).saveAll(captor.capture());
        var captured = captor.getValue();
        assertEquals(1, captured.size());
        assertEquals(output.id(), captured.getFirst().getFoodId());
        assertEquals("Colher", captured.getFirst().getLabel());
    }
}

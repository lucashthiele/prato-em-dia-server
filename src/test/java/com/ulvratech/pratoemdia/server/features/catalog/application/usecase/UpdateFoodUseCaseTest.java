package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.UpdateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateFoodUseCaseTest {
    private FoodRepository repository;
    private UpdateFoodUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(FoodRepository.class);
        useCase = new UpdateFoodUseCase(repository);
    }

    @Test
    void allowsAuthorToUpdateFood() {
        UUID author = UUID.randomUUID();
        var food = Food.create("Arroz", null, "g", null, null, author);
        when(repository.findById(food.getId())).thenReturn(Optional.of(food));
        when(repository.save(food)).thenReturn(food);

        var output = useCase.execute(input(food, author, false, "Arroz integral"));

        assertEquals("Arroz integral", output.name());
        verify(repository).save(food);
    }

    @Test
    void allowsAdministratorToUpdateAnotherUsersFood() {
        var food = Food.create("Arroz", null, "g", null, null, UUID.randomUUID());
        when(repository.findById(food.getId())).thenReturn(Optional.of(food));
        when(repository.save(food)).thenReturn(food);

        assertDoesNotThrow(
            () -> useCase.execute(input(food, UUID.randomUUID(), true, "Arroz integral"))
        );
    }

    @Test
    void rejectsUpdateFromAnotherNonAdminUser() {
        var food = Food.create("Arroz", null, "g", null, null, UUID.randomUUID());
        when(repository.findById(food.getId())).thenReturn(Optional.of(food));

        assertThrows(
            FoodAccessDeniedException.class,
            () -> useCase.execute(input(food, UUID.randomUUID(), false, "Outro nome"))
        );
        verify(repository, never()).save(any());
    }

    private UpdateFoodInput input(Food food, UUID actor, boolean admin, String name) {
        return new UpdateFoodInput(
            food.getId(), actor, admin, name, food.getCategory(), food.getDefaultUnit(),
            food.getNotes(), food.getPreparationMethod(), food.isActive()
        );
    }
}

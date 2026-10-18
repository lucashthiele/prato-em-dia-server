package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ListFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.junit.jupiter.api.Test;

class ListFoodsUseCaseTest {
    private final ListFoodsUseCase useCase = new ListFoodsUseCase(mock(FoodRepository.class));

    @Test
    void rejectsSearchTermThatIsEmptyAfterNormalization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(new ListFoodsInput(0, 20, " !? "))
        );
    }
}

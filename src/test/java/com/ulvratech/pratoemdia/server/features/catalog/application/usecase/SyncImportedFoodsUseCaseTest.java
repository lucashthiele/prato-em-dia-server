package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncImportedFoodsUseCaseTest {
    @Test
    void insertsThenSkipsAndUpdatesTheSameSourceFood() {
        var repository = mock(FoodRepository.class);
        when(repository.findBySourceAndExternalIds(anyString(), anyCollection()))
            .thenReturn(List.of());
        when(repository.saveAll(anyCollection())).thenAnswer(call -> List.copyOf(call.getArgument(0)));
        var useCase = new SyncImportedFoodsUseCase(repository);

        var first = useCase.execute(input("Arroz, integral, cozido", false));
        @SuppressWarnings("unchecked")
        var saved = (List<Food>) mockingDetails(repository).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("saveAll"))
            .findFirst().orElseThrow().getArgument(0);
        Food imported = saved.getFirst();
        assertEquals(1, first.inserted());

        when(repository.findBySourceAndExternalIds(anyString(), anyCollection()))
            .thenReturn(List.of(imported));
        assertEquals(1, useCase.execute(input("Arroz, integral, cozido", false)).skipped());
        assertEquals(1, useCase.execute(input("Arroz integral, cozido", false)).updated());
        assertEquals("Arroz integral, cozido", imported.getSourceDescription());
        assertEquals(CatalogIds.tacoFood("1"), imported.getId());
    }

    @Test
    void dryRunNeverWrites() {
        var repository = mock(FoodRepository.class);
        when(repository.findBySourceAndExternalIds(anyString(), anyCollection())).thenReturn(List.of());

        var result = new SyncImportedFoodsUseCase(repository).execute(input("Arroz", true));

        assertEquals(1, result.inserted());
        verify(repository, never()).saveAll(anyCollection());
    }

    private SyncImportedFoodsInput input(String description, boolean dryRun) {
        var nutrients = Map.of("energy_kcal", new NutrientValue(
            new BigDecimal("123.5"), "kcal", NutrientValueStatus.VALUE, "123.5"
        ));
        return new SyncImportedFoodsInput(
            "TACO", "TACO 4a edicao (2011)",
            List.of(new ImportedFoodInput("1", description, "Cereais", nutrients)), dryRun
        );
    }
}

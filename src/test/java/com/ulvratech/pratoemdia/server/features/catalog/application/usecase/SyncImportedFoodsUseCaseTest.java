package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ImportedFoodPortionInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SyncImportedFoodsUseCaseTest {
  @Test
  void insertsThenSkipsAndUpdatesTheSameSourceFoodAndItsPortions() {
    var repository = mock(FoodRepository.class);
    var portionRepository = mock(FoodPortionRepository.class);
    when(repository.findBySourceAndExternalIds(anyString(), anyCollection())).thenReturn(List.of());
    when(portionRepository.findImportedByFoodIds(anyCollection())).thenReturn(List.of());
    var useCase = new SyncImportedFoodsUseCase(repository, portionRepository);

    var first = useCase.execute(input("Arroz, integral, cozido", "123.5", false));
    @SuppressWarnings("unchecked")
    var savedFoods =
        (List<Food>)
            mockingDetails(repository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("saveAll"))
                .findFirst()
                .orElseThrow()
                .getArgument(0);
    @SuppressWarnings("unchecked")
    var savedPortions =
        (List<FoodPortion>)
            mockingDetails(portionRepository).getInvocations().stream()
                .filter(
                    invocation ->
                        invocation.getMethod().getName().equals("replaceImportedForFoods"))
                .findFirst()
                .orElseThrow()
                .getArgument(1);
    Food imported = savedFoods.getFirst();
    assertEquals(1, first.inserted());

    when(repository.findBySourceAndExternalIds(anyString(), anyCollection()))
        .thenReturn(List.of(imported));
    when(portionRepository.findImportedByFoodIds(anyCollection())).thenReturn(savedPortions);
    assertEquals(1, useCase.execute(input("Arroz, integral, cozido", "123.5", false)).skipped());
    assertEquals(1, useCase.execute(input("Arroz integral, cozido", "124", false)).updated());
    assertEquals("Arroz integral, cozido", imported.getSourceDescription());
    assertEquals(CatalogIds.tacoFood("1"), imported.getId());
  }

  @Test
  void dryRunNeverWrites() {
    var repository = mock(FoodRepository.class);
    var portionRepository = mock(FoodPortionRepository.class);
    when(repository.findBySourceAndExternalIds(anyString(), anyCollection())).thenReturn(List.of());
    when(portionRepository.findImportedByFoodIds(anyCollection())).thenReturn(List.of());

    var result =
        new SyncImportedFoodsUseCase(repository, portionRepository)
            .execute(input("Arroz", "123.5", true));

    assertEquals(1, result.inserted());
    verify(repository, never()).saveAll(anyCollection());
    verify(portionRepository, never()).replaceImportedForFoods(anyCollection(), anyCollection());
  }

  private SyncImportedFoodsInput input(String description, String amount, boolean dryRun) {
    var nutrients =
        Map.of(
            "energy_kcal",
            new NutrientValue(new BigDecimal(amount), "kcal", NutrientValueStatus.VALUE, amount));
    var portion =
        new ImportedFoodPortionInput(
            "STANDARD", "100 g", "Valor por 100g", new BigDecimal("100"), "g", nutrients);
    return new SyncImportedFoodsInput(
        "TACO",
        "TACO 4a edicao (2011)",
        List.of(new ImportedFoodInput("1", description, "Cereais", null, List.of(portion))),
        dryRun);
  }
}

package com.ulvratech.pratoemdia.server.features.catalog.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodDetailsOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodPortionOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodQueryInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ListFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.UpdateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CreateFood;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.CreateFoodPortion;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.GetFood;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.ListFoodPortions;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.ListFoods;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.UpdateFood;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.CreateFoodPortionRequest;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.CreateFoodRequest;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.UpdateFoodRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/foods")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Alimentos", description = "Consulta e manutenção do catálogo de alimentos.")
public class FoodController {
    private final CreateFood createFood;
    private final UpdateFood updateFood;
    private final GetFood getFood;
    private final ListFoods listFoods;
    private final CreateFoodPortion createPortion;
    private final ListFoodPortions listPortions;

    public FoodController(
            CreateFood createFood,
            UpdateFood updateFood,
            GetFood getFood,
            ListFoods listFoods,
            CreateFoodPortion createPortion,
            ListFoodPortions listPortions
    ) {
        this.createFood = createFood;
        this.updateFood = updateFood;
        this.getFood = getFood;
        this.listFoods = listFoods;
        this.createPortion = createPortion;
        this.listPortions = listPortions;
    }

    @GetMapping
    @Operation(summary = "Listar alimentos", description = "Retorna uma página do catálogo de alimentos.")
    PageResult<FoodOutput> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return listFoods.execute(new ListFoodsInput(page, size));
    }

    @GetMapping("/{foodId}")
    @Operation(summary = "Consultar alimento", description = "Retorna os detalhes e as porções de um alimento pelo identificador.")
    FoodDetailsOutput get(@PathVariable UUID foodId) {
        return getFood.execute(new FoodQueryInput(foodId));
    }

    @PostMapping
    @Operation(summary = "Criar alimento", description = "Adiciona um alimento e suas porções iniciais ao catálogo.")
    ResponseEntity<FoodOutput> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateFoodRequest request
    ) {
        var output = createFood.execute(
                new CreateFoodInput(
                        userId(jwt), FoodOrigin.valueOf(jwt.getClaimAsString("role")),
                        request.name(), request.category(), request.defaultUnit(),
                        request.notes(), request.preparationMethod(), request.portions().stream()
                                .map(
                                        portion -> new CreateFoodPortionInput(
                                                portion.label(), portion.quantity(), portion.unit(),
                                                portion.gramsEquivalent()
                                        )
                                )
                                .toList()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PutMapping("/{foodId}")
    @Operation(summary = "Atualizar alimento", description = "Substitui os dados editáveis de um alimento existente.")
    FoodOutput update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID foodId,
            @Valid @RequestBody UpdateFoodRequest request
    ) {
        return updateFood.execute(
                new UpdateFoodInput(
                        foodId, userId(jwt), "ADMIN".equals(jwt.getClaimAsString("role")),
                        request.name(), request.category(), request.defaultUnit(), request.notes(),
                        request.preparationMethod(), request.active()
                )
        );
    }

    @GetMapping("/{foodId}/portions")
    @Operation(summary = "Listar porções do alimento", description = "Retorna as porções cadastradas para o alimento informado.")
    List<FoodPortionOutput> portions(@PathVariable UUID foodId) {
        return listPortions.execute(new FoodQueryInput(foodId));
    }

    @PostMapping("/{foodId}/portions")
    @Operation(summary = "Criar porções do alimento", description = "Adiciona uma ou mais porções a um alimento existente.")
    ResponseEntity<List<FoodPortionOutput>> createPortions(
            @PathVariable UUID foodId,
            @RequestBody @NotEmpty(message = "Informe ao menos uma porção.") List<@NotNull(message = "A porção não pode ser nula.") @Valid CreateFoodPortionRequest> request
    ) {
        var portions = request.stream()
                .map(
                        portion -> new CreateFoodPortionInput(
                                portion.label(), portion.quantity(), portion.unit(), portion.gramsEquivalent()
                        )
                )
                .toList();
        var output = createPortion.execute(new CreateFoodPortionsInput(foodId, portions));
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

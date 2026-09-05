package com.ulvratech.pratoemdia.server.features.catalog.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.*;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.CreateFoodPortionRequest;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.CreateFoodRequest;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.UpdateFoodRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/foods")
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
    PageResult<FoodOutput> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return listFoods.execute(new ListFoodsInput(page, size));
    }

    @GetMapping("/{foodId}")
    FoodDetailsOutput get(@PathVariable UUID foodId) {
        return getFood.execute(new FoodQueryInput(foodId));
    }

    @PostMapping
    ResponseEntity<FoodOutput> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateFoodRequest request
    ) {
        var output = createFood.execute(new CreateFoodInput(
            userId(jwt), request.name(), request.category(), request.defaultUnit(),
            request.notes(), request.preparationMethod(), request.portions().stream()
                .map(portion -> new CreateFoodPortionInput(
                    portion.label(), portion.quantity(), portion.unit(), portion.gramsEquivalent()
                ))
                .toList()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    @PutMapping("/{foodId}")
    FoodOutput update(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID foodId,
        @Valid @RequestBody UpdateFoodRequest request
    ) {
        return updateFood.execute(new UpdateFoodInput(
            foodId, userId(jwt), "ADMIN".equals(jwt.getClaimAsString("role")),
            request.name(), request.category(), request.defaultUnit(), request.notes(),
            request.preparationMethod(), request.active()
        ));
    }

    @GetMapping("/{foodId}/portions")
    List<FoodPortionOutput> portions(@PathVariable UUID foodId) {
        return listPortions.execute(new FoodQueryInput(foodId));
    }

    @PostMapping("/{foodId}/portions")
    ResponseEntity<List<FoodPortionOutput>> createPortions(
        @PathVariable UUID foodId,
        @RequestBody @NotEmpty(message = "Informe ao menos uma porção.")
        List<@NotNull(message = "A porção não pode ser nula.") @Valid CreateFoodPortionRequest> request
    ) {
        var portions = request.stream()
            .map(portion -> new CreateFoodPortionInput(
                portion.label(), portion.quantity(), portion.unit(), portion.gramsEquivalent()
            ))
            .toList();
        var output = createPortion.execute(new CreateFoodPortionsInput(foodId, portions));
        return ResponseEntity.status(HttpStatus.CREATED).body(output);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

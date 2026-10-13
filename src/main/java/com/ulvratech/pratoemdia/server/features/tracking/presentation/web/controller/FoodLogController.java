package com.ulvratech.pratoemdia.server.features.tracking.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes;

import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.FoodLogOperations;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planned-meals")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class FoodLogController {
    private final FoodLogOperations operations;

    public FoodLogController(FoodLogOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/{plannedMealId}/food-log")
    @Operation(summary = "Contexto da refeição planejada", description = "Retorna itens planejados, modo e substituições autorizadas para o registro alimentar.")
    public FoodLogTypes.FoodLog get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID plannedMealId
    ) {
        return operations.get(
                new FoodLogTypes.Actor(
                        UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role")
                ), plannedMealId
        );
    }
}

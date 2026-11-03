package com.ulvratech.pratoemdia.server.features.shopping.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput;
import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput.Actor;
import com.ulvratech.pratoemdia.server.features.shopping.application.port.in.ShoppingListOperations;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/me/weekly-plans/{planId}/shopping-list")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Lista de compras")
public class ShoppingListController {
    private final ShoppingListOperations shopping;

    public ShoppingListController(ShoppingListOperations shopping) {
        this.shopping = shopping;
    }

    @PostMapping
    @Operation(summary = "Criar a lista de compras do plano", description = "Soma os alimentos das refeições planejadas por alimento, porção e unidade. Idempotente: 201 ao criar, 200 se o plano já tem lista ativa. Qualquer mudança nas refeições planejadas desativa a lista.")
    public ResponseEntity<ShoppingListOutput> create(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId
    ) {
        var result = shopping.create(actor(jwt), planId);
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.list());
    }

    @GetMapping
    @Operation(summary = "Consultar a lista de compras ativa do plano", description = "Devolve 404 (SHOPPING_LIST_NOT_FOUND) quando não há lista ativa; o cliente então chama o POST.")
    public ShoppingListOutput get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        return shopping.get(actor(jwt), planId);
    }

    private Actor actor(Jwt jwt) {
        return new Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

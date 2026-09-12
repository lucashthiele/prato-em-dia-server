package com.ulvratech.pratoemdia.server.features.catalog.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.*;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FoodControllerTest {
    @Test
    void delegatesEveryCatalogEndpoint() {
        var create = mock(CreateFood.class); var update = mock(UpdateFood.class);
        var get = mock(GetFood.class); var list = mock(ListFoods.class);
        var createPortion = mock(CreateFoodPortion.class);
        var listPortions = mock(ListFoodPortions.class);
        var controller = new FoodController(create, update, get, list, createPortion, listPortions);
        var id = UUID.randomUUID();
        var portion = new CreateFoodPortionRequest("100 g", BigDecimal.valueOf(100), "g",
            BigDecimal.valueOf(100));

        controller.list(1, 10);
        controller.get(id);
        var created = controller.create(jwt(UUID.randomUUID(), "ADMIN"),
            new CreateFoodRequest("Food", "Category", "g", "notes", "method", List.of(portion)));
        controller.update(jwt(UUID.randomUUID(), "ADMIN"), id,
            new UpdateFoodRequest("Food 2", "Category", "g", null, null, true));
        controller.portions(id);
        var portions = controller.createPortions(id, List.of(portion));

        assertEquals(201, created.getStatusCode().value());
        assertEquals(201, portions.getStatusCode().value());
        verify(list).execute(new ListFoodsInput(1, 10));
        verify(get).execute(new FoodQueryInput(id));
        verify(create).execute(any(CreateFoodInput.class));
        verify(update).execute(any(UpdateFoodInput.class));
        verify(listPortions).execute(new FoodQueryInput(id));
        verify(createPortion).execute(any(CreateFoodPortionsInput.class));
    }

    @Test
    void createFoodRequestDefaultsNullPortions() {
        assertTrue(new CreateFoodRequest("Food", null, "g", null, null, null)
            .portions().isEmpty());
    }

    private static Jwt jwt(UUID id, String role) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(id.toString())
            .claim("role", role).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
            .build();
    }
}

package com.ulvratech.pratoemdia.server.features.catalog.presentation.exception;

import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodNotFoundException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogExceptionHandlerTest {
    @Test
    void mapsCatalogErrors() {
        var handler = new CatalogExceptionHandler();
        assertEquals(404, handler.notFound(new FoodNotFoundException()).getStatusCode().value());
        assertEquals(403, handler.forbidden(new FoodAccessDeniedException()).getStatusCode().value());
        assertEquals("CATALOG_INVALID_VALUE", handler.invalid().getBody().code());
    }
}

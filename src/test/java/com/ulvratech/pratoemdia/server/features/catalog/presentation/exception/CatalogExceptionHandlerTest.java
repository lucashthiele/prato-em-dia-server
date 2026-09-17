package com.ulvratech.pratoemdia.server.features.catalog.presentation.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.ulvratech.pratoemdia.server.features.catalog.application.exception.*;
import org.junit.jupiter.api.Test;

class CatalogExceptionHandlerTest {
  @Test
  void mapsCatalogErrors() {
    var handler = new CatalogExceptionHandler();
    assertEquals(404, handler.notFound(new FoodNotFoundException()).getStatusCode().value());
    assertEquals(403, handler.forbidden(new FoodAccessDeniedException()).getStatusCode().value());
    assertEquals("CATALOG_INVALID_VALUE", handler.invalid().getBody().code());
  }
}

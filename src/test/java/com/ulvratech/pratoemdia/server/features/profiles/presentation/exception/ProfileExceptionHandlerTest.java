package com.ulvratech.pratoemdia.server.features.profiles.presentation.exception;

import com.ulvratech.pratoemdia.server.features.profiles.application.exception.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileExceptionHandlerTest {
    @Test void mapsProfileErrors() {
        var handler = new ProfileExceptionHandler();
        assertEquals(404, handler.handleNotFound(new ProfileNotFoundException()).getStatusCode().value());
        assertEquals(403, handler.handleNotAvailable(new ProfileNotAvailableException()).getStatusCode().value());
        assertEquals(400, handler.handleInvalidFields(new InvalidProfileFieldsException()).getStatusCode().value());
        assertEquals("PROFILE_INVALID_VALUE", handler.handleInvalidValue().getBody().code());
    }
}

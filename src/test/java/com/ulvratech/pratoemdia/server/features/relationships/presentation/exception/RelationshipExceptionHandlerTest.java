package com.ulvratech.pratoemdia.server.features.relationships.presentation.exception;

import com.ulvratech.pratoemdia.server.features.relationships.application.exception.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationshipExceptionHandlerTest {
    @Test void mapsRelationshipErrors() {
        var handler = new RelationshipExceptionHandler();
        assertEquals(404, handler.notFound(new RelationshipNotFoundException()).getStatusCode().value());
        assertEquals(409, handler.conflict(new RelationshipConflictException()).getStatusCode().value());
        assertEquals(503, handler.email(new RelationshipEmailException()).getStatusCode().value());
    }
}

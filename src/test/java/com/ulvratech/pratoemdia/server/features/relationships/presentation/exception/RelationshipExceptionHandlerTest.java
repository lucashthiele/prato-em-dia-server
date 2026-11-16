package com.ulvratech.pratoemdia.server.features.relationships.presentation.exception;

import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipEmailException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipExceptionHandlerTest {
    @Test
    void mapsRelationshipErrors() {
        var handler = new RelationshipExceptionHandler();
        assertEquals(404, handler.notFound(new RelationshipNotFoundException()).getStatusCode().value());
        assertEquals(409, handler.conflict(new RelationshipConflictException()).getStatusCode().value());
        assertEquals(503, handler.email(new RelationshipEmailException()).getStatusCode().value());
    }

    @Test
    void alreadyExistsCarriesPatientId() {
        var patientId = UUID.randomUUID();
        var response = new RelationshipExceptionHandler()
                .alreadyExists(new RelationshipAlreadyExistsException(patientId));
        assertEquals(409, response.getStatusCode().value());
        var body = response.getBody();
        assertEquals("RELATIONSHIP_ALREADY_EXISTS", body.code());
        assertEquals("patientId", body.errors().get(0).field());
        assertEquals(patientId.toString(), body.errors().get(0).message());
    }
}

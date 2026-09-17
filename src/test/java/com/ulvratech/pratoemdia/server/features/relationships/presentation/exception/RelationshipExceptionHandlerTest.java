package com.ulvratech.pratoemdia.server.features.relationships.presentation.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipEmailException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import org.junit.jupiter.api.Test;

class RelationshipExceptionHandlerTest {
  @Test
  void mapsRelationshipErrors() {
    var handler = new RelationshipExceptionHandler();
    assertEquals(
        404, handler.notFound(new RelationshipNotFoundException()).getStatusCode().value());
    assertEquals(
        409, handler.conflict(new RelationshipConflictException()).getStatusCode().value());
    assertEquals(503, handler.email(new RelationshipEmailException()).getStatusCode().value());
  }
}

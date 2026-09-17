package com.ulvratech.pratoemdia.server.features.relationships.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class RelationshipConflictException extends CodedApplicationException {
  public RelationshipConflictException() {
    super("RELATIONSHIP_CONFLICT");
  }
}

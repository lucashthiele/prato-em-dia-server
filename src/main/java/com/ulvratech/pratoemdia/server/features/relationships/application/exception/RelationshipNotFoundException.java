package com.ulvratech.pratoemdia.server.features.relationships.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class RelationshipNotFoundException extends CodedApplicationException {
  public RelationshipNotFoundException() {
    super("RELATIONSHIP_NOT_FOUND");
  }
}

package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class InvalidCredentialsException extends CodedApplicationException {
  public InvalidCredentialsException() {
    super("AUTH_INVALID_CREDENTIALS");
  }
}

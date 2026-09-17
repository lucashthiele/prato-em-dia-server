package com.ulvratech.pratoemdia.server.features.profiles.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class InvalidProfileFieldsException extends CodedApplicationException {
  public InvalidProfileFieldsException() {
    super("PROFILE_FIELDS_NOT_ALLOWED");
  }
}

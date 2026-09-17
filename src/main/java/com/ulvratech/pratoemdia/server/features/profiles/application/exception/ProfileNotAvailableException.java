package com.ulvratech.pratoemdia.server.features.profiles.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ProfileNotAvailableException extends CodedApplicationException {
  public ProfileNotAvailableException() {
    super("PROFILE_NOT_AVAILABLE");
  }
}

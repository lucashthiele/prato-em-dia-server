package com.ulvratech.pratoemdia.server.features.profiles.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ProfileNotFoundException extends CodedApplicationException {
  public ProfileNotFoundException() {
    super("PROFILE_NOT_FOUND");
  }
}

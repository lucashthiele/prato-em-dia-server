package com.ulvratech.pratoemdia.server.features.identity.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class InactiveUserAccountException extends CodedApplicationException {
  public InactiveUserAccountException() {
    super("AUTH_ACCOUNT_INACTIVE");
  }
}

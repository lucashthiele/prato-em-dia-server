package com.ulvratech.pratoemdia.server.features.catalog.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class FoodAccessDeniedException extends CodedApplicationException {
  public FoodAccessDeniedException() {
    super("FOOD_ACCESS_DENIED");
  }
}

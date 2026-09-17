package com.ulvratech.pratoemdia.server.features.catalog.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class FoodNotFoundException extends CodedApplicationException {
  public FoodNotFoundException() {
    super("FOOD_NOT_FOUND");
  }
}

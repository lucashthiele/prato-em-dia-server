package com.ulvratech.pratoemdia.server.features.shopping.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ShoppingPlanNotFoundException extends CodedApplicationException {
    public ShoppingPlanNotFoundException() {
        super("SHOPPING_PLAN_NOT_FOUND");
    }
}

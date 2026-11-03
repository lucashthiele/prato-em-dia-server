package com.ulvratech.pratoemdia.server.features.shopping.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ShoppingListNotFoundException extends CodedApplicationException {
    public ShoppingListNotFoundException() {
        super("SHOPPING_LIST_NOT_FOUND");
    }
}

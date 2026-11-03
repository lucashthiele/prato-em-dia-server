package com.ulvratech.pratoemdia.server.features.shopping.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

public class ShoppingAccessDeniedException extends CodedApplicationException {
    public ShoppingAccessDeniedException() {
        super("SHOPPING_ACCESS_DENIED");
    }
}

package com.ulvratech.pratoemdia.server.features.shopping.domain.entity;

/** {@code created} is false when the plan already had an active list. */
public record ShoppingListCreation(boolean created, ShoppingList list) {
}

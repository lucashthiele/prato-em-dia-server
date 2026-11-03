package com.ulvratech.pratoemdia.server.features.shopping.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** Total to buy of a food, in a portion and unit. The app keys "bought" by these three values. */
public record ShoppingListItem(UUID foodId, UUID foodPortionId, String unit, BigDecimal quantity) {
}

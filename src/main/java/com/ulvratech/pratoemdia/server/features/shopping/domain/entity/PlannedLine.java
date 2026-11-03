package com.ulvratech.pratoemdia.server.features.shopping.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** One planned item of the plan, as read for the shopping list. */
public record PlannedLine(UUID foodId, UUID foodPortionId, String unit, BigDecimal quantity) {
}

package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

import java.math.BigDecimal;

public record CreateFoodPortionInput(
    String label, BigDecimal quantity, String unit, BigDecimal gramsEquivalent) {}

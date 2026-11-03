package com.ulvratech.pratoemdia.server.features.prescriptions.application.service;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.Macros;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Sum of known macros; any unknown macro marks the result as incomplete. */
public final class MacroSum {
    public static final int DISPLAY_SCALE = 2;

    private BigDecimal kcal = BigDecimal.ZERO;
    private BigDecimal protein = BigDecimal.ZERO;
    private BigDecimal carbohydrate = BigDecimal.ZERO;
    private BigDecimal lipids = BigDecimal.ZERO;
    private boolean complete = true;

    public void add(Macros macros) {
        kcal = kcal.add(orZero(macros.energyKcal()));
        protein = protein.add(orZero(macros.protein()));
        carbohydrate = carbohydrate.add(orZero(macros.carbohydrate()));
        lipids = lipids.add(orZero(macros.lipids()));
        complete &= macros.complete();
    }

    public Macros result() {
        return new Macros(round(kcal), round(protein), round(carbohydrate), round(lipids), complete);
    }

    public static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

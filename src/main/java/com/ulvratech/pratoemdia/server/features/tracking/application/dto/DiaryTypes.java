package com.ulvratech.pratoemdia.server.features.tracking.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DiaryTypes {
    private DiaryTypes() {
    }

    public record Actor(UUID accountId, String role) {
    }

    public record PlanPeriod(UUID planId, UUID prescriptionVersionId, LocalDate start, LocalDate end) {
    }

    public record PrescriptionPeriod(UUID prescriptionId, UUID versionId, LocalDate start, LocalDate end) {
    }

    public record Calendar(List<PlanPeriod> plans, List<PrescriptionPeriod> prescriptions,
            List<LocalDate> freeExecutionDates) {
    }

    public record NutrientTotal(BigDecimal amount, String unit) {
    }

    public record DailySummary(LocalDate date, UUID planId, UUID prescriptionVersionId,
            Map<String, NutrientTotal> prescribed, Map<String, NutrientTotal> planned,
            Map<String, NutrientTotal> realized) {
    }
}

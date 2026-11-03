package com.ulvratech.pratoemdia.server.features.prescriptions.application.dto;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Leitura da prescrição do paciente para o portal: nomes de alimento e macros já calculados.
 * Os totais são de um dia da prescrição.
 */
public record PrescriptionSummary(
        @Schema(description = "Nulo quando o paciente não tem prescrição") UUID prescriptionId,
        PlanningMode planningMode,
        @Schema(description = "Versão publicada vigente, se houver") VersionSummary current,
        @Schema(description = "Rascunho aberto, se houver") VersionSummary draft,
        @Schema(description = "Metadados de todas as versões, da mais nova para a mais antiga") List<HistoryEntry> history) {

    @Schema(description = "Macros em gramas (kcal em energyKcal), arredondados em duas casas. Um macro nulo não pôde ser calculado; complete=false indica que o valor não considera todos os itens.")
    public record Macros(
            BigDecimal energyKcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal lipids,
            boolean complete) {
    }

    public record TargetSummary(String nutrientCode, BigDecimal amount, String unit) {
    }

    public record SubstitutionSummary(
            UUID id, UUID foodId, String foodName, BigDecimal quantity, String unit,
            String portionLabel, String notes) {
    }

    public record ItemSummary(
            UUID id, UUID foodId, String foodName, BigDecimal quantity, String unit,
            String portionLabel, String notes, Macros nutrients,
            List<SubstitutionSummary> substitutions) {
    }

    public record MealSummary(
            UUID id, String name, MealType mealType, int order, LocalTime suggestedTime,
            String notes, Macros totals, List<ItemSummary> items) {
    }

    public record VersionSummary(
            UUID id, int number, PrescriptionVersionStatus status, String title, String notes,
            Instant createdAt, Instant publishedAt, Macros totals, List<TargetSummary> targets,
            List<MealSummary> meals) {
    }

    public record HistoryEntry(
            UUID id, int number, PrescriptionVersionStatus status, String title, Instant createdAt,
            Instant publishedAt, Instant archivedAt) {
    }
}

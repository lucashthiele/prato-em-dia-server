package com.ulvratech.pratoemdia.server.features.tracking.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.ConsumedItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Registro append-only de realização. O servidor deriva tipo, modo, conversões e snapshots.")
public record CreateCompletionEventRequest(
        @Schema(description = "Motivo opcional informado pelo paciente") String reason,
        @Schema(description = "Instante da realização; a data em America/Sao_Paulo deve pertencer ao plano", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-18T15:00:00Z") @NotNull Instant occurredAt,
        @Schema(description = "Composição declarada; vazia representa não realizada. Quando não vazia, contém uma linha por item planejado.") List<@Valid ConsumedItemRequest> consumedItems) {
    public record ConsumedItemRequest(
            @NotNull UUID catalogFoodId,
            @NotNull UUID catalogPortionId,
            @NotBlank String unit,
            @NotNull @DecimalMin("0.0") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal multiplier,
            UUID plannedItemId,
            UUID allowedSubstitutionId) {
        public ConsumedItem toConsumedItem() {
            return new ConsumedItem(
                    catalogFoodId, catalogPortionId, unit, quantity, multiplier,
                    plannedItemId, allowedSubstitutionId
            );
        }
    }
}

package com.ulvratech.pratoemdia.server.features.tracking.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Choice;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Registro append-only de realização. Em SUBSTITUTED, informe uma alternativa por item trocado.")
public record CreateCompletionEventRequest(
        @Schema(description = "Estado registrado para a refeição", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUBSTITUTED") @NotNull Type eventType,
        @Schema(description = "Motivo opcional informado pelo paciente") String reason,
        @Schema(description = "Instante da realização; a data em America/Sao_Paulo deve pertencer ao plano", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-18T15:00:00Z") @NotNull Instant occurredAt,
        @Schema(description = "Uma alternativa por item planejado trocado; obrigatório apenas em SUBSTITUTED") List<@Valid ChoiceRequest> substitutions) {
    public record ChoiceRequest(
            @Schema(description = "ID do item no planejamento", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID plannedMealItemId,
            @Schema(description = "ID da alternativa permitida na versão da prescrição", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID allowedSubstitutionId) {
        public Choice toChoice() {
            return new Choice(plannedMealItemId, allowedSubstitutionId);
        }
    }
}

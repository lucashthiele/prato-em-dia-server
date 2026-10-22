package com.ulvratech.pratoemdia.server.features.summary.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.summary.application.dto.GetSummaryInput;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput;
import com.ulvratech.pratoemdia.server.features.summary.application.port.in.GetNutritionistSummary;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/nutritionists/me/summary")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Resumo do nutricionista", description = "Indicadores agregados para o início do portal do nutricionista.")
public class NutritionistSummaryController {
    private final GetNutritionistSummary summary;

    public NutritionistSummaryController(GetNutritionistSummary summary) {
        this.summary = summary;
    }

    @GetMapping
    @Operation(summary = "Resumo do nutricionista", description = "Retorna contagens (pacientes ativos, acessos pendentes e com falha, rascunhos em aberto) e os rascunhos de prescrição mais recentes, considerando somente pacientes com vínculo ativo.")
    NutritionistSummaryOutput get(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int draftLimit
    ) {
        return summary.execute(new GetSummaryInput(UUID.fromString(jwt.getSubject()), draftLimit));
    }
}

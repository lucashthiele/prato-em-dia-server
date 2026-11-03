package com.ulvratech.pratoemdia.server.features.tracking.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.DiaryOperations;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only mirror of the patient's diary reads, for a nutritionist with an active relationship. */
@RestController
@RequestMapping("/api/v1/nutritionists/me/patients/{patientId}")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Diário do paciente (nutricionista)")
public class NutritionistDiaryController {
    private final DiaryOperations diary;
    private final ExecutionOperations executions;

    public NutritionistDiaryController(DiaryOperations diary, ExecutionOperations executions) {
        this.diary = diary;
        this.executions = executions;
    }

    @GetMapping("/meal-executions")
    @Operation(summary = "Listar execuções do paciente por período", description = "Execuções ativas entre from e to (inclusive, no máximo 92 dias). Somente leitura.")
    public List<ExecutionTypes.Execution> listExecutions(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return executions.list(executionActor(jwt), patientId, from, to);
    }

    @GetMapping("/meal-executions/{id}")
    @Operation(summary = "Consultar execução do paciente", description = "Retorna uma execução do paciente com itens e totais congelados. Somente leitura.")
    public ExecutionTypes.Execution getExecution(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId, @PathVariable UUID id
    ) {
        return executions.get(executionActor(jwt), patientId, id);
    }

    @GetMapping("/daily-meal-categories")
    @Operation(summary = "Estado e permissão das categorias do paciente no dia", description = "Mesma resposta de /patients/me/daily-meal-categories, para o paciente vinculado.")
    public DiaryTypes.DailyMealCategories dailyMealCategories(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return diary.dailyMealCategories(actor(jwt), patientId, date);
    }

    @GetMapping("/daily-nutrition-summary")
    @Operation(summary = "Resumo nutricional diário do paciente", description = "Mesma resposta de /patients/me/daily-nutrition-summary, para o paciente vinculado.")
    public DiaryTypes.DailySummary dailySummary(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return diary.dailySummary(actor(jwt), patientId, date);
    }

    @GetMapping("/calendar-periods")
    @Operation(summary = "Períodos consultáveis do calendário do paciente", description = "Mesma resposta de /patients/me/calendar-periods, para o paciente vinculado.")
    public DiaryTypes.Calendar calendar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        return diary.calendar(actor(jwt), patientId);
    }

    private ExecutionTypes.Actor executionActor(Jwt jwt) {
        return new ExecutionTypes.Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }

    private DiaryTypes.Actor actor(Jwt jwt) {
        return new DiaryTypes.Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

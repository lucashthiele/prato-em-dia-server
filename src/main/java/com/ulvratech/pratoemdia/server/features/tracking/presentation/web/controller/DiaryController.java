package com.ulvratech.pratoemdia.server.features.tracking.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;

import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.DiaryOperations;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.List;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/me")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Diário alimentar")
public class DiaryController {
    private final DiaryOperations diary;
    private final ExecutionOperations executions;

    public DiaryController(DiaryOperations diary, ExecutionOperations executions) {
        this.diary = diary;
        this.executions = executions;
    }

    @GetMapping("/calendar-periods")
    @Operation(summary = "Períodos consultáveis do calendário", description = "Retorna períodos dos planos e prescrições, além de datas com execuções livres.")
    public DiaryTypes.Calendar calendar(@AuthenticationPrincipal Jwt jwt) {
        return diary.calendar(actor(jwt));
    }

    @GetMapping("/daily-nutrition-summary")
    @Operation(summary = "Resumo nutricional diário", description = "Separa metas prescritas, composição planejada e consumo realizado para a data.")
    public DiaryTypes.DailySummary dailySummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return diary.dailySummary(actor(jwt), date);
    }

    @PostMapping("/meal-executions")
    @Operation(summary = "Criar execução de refeição", description = "Valida o modo da prescrição ou do plano e salva itens com snapshots nutricionais.")
    public ResponseEntity<ExecutionTypes.Execution> createExecution(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ExecutionTypes.Write input
    ) {
        return ResponseEntity.status(201).body(executions.create(executionActor(jwt), key, input));
    }

    @GetMapping("/meal-executions/{id}")
    @Operation(summary = "Consultar execução de refeição", description = "Retorna uma execução do paciente com seus itens e totais congelados.")
    public ExecutionTypes.Execution getExecution(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return executions.get(executionActor(jwt), id);
    }

    @GetMapping("/meal-executions")
    @Operation(summary = "Listar execuções de uma data", description = "Retorna as execuções ativas do paciente na data informada.")
    public List<ExecutionTypes.Execution> listExecutions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return executions.list(executionActor(jwt), date);
    }

    @PutMapping("/meal-executions/{id}")
    @Operation(summary = "Substituir execução de refeição", description = "Substitui os itens com controle de versão e chave de idempotência.")
    public ExecutionTypes.Execution replaceExecution(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader("If-Match") long version,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ExecutionTypes.Write input
    ) {
        return executions.replace(executionActor(jwt), id, version, key, input);
    }

    @PostMapping("/meal-executions/{id}/move")
    @Operation(summary = "Realocar execução de refeição", description = "Move a execução de forma atômica após validar o novo destino e modo.")
    public ExecutionTypes.Execution moveExecution(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader("If-Match") long version,
            @RequestHeader("Idempotency-Key") String key, @RequestBody ExecutionTypes.Write input
    ) {
        return executions.move(executionActor(jwt), id, version, key, input);
    }

    @DeleteMapping("/meal-executions/{id}")
    @Operation(summary = "Remover execução com janela de restauração de cinco segundos", description = "Marca a execução como removida e devolve token válido por cinco segundos.")
    public ExecutionTypes.Deleted deleteExecution(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader("If-Match") long version,
            @RequestHeader("Idempotency-Key") String key
    ) {
        return executions.delete(executionActor(jwt), id, version, key);
    }

    public record RestoreRequest(UUID restoreToken) {
    }

    @PostMapping("/meal-executions/{id}/restore")
    @Operation(summary = "Restaurar execução removida", description = "Restaura uma execução removida dentro da janela usando o token devolvido.")
    public ExecutionTypes.Execution restoreExecution(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestBody RestoreRequest input
    ) {
        return executions.restore(executionActor(jwt), id, input.restoreToken());
    }

    private ExecutionTypes.Actor executionActor(Jwt jwt) {
        return new ExecutionTypes.Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }

    private DiaryTypes.Actor actor(Jwt jwt) {
        return new DiaryTypes.Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

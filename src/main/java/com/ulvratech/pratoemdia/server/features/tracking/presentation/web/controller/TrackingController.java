package com.ulvratech.pratoemdia.server.features.tracking.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Create;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Event;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Summary;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.TrackingOperations;
import com.ulvratech.pratoemdia.server.features.tracking.presentation.web.request.CreateCompletionEventRequest;
import com.ulvratech.pratoemdia.server.features.tracking.presentation.web.request.CreateCompletionEventRequest.ChoiceRequest;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Realização de refeições")
public class TrackingController {
    private final TrackingOperations operations;

    public TrackingController(TrackingOperations operations) {
        this.operations = operations;
    }

    @PostMapping("/planned-meals/{plannedMealId}/completion-events")
    @Operation(summary = "Registrar realização", description = "Adiciona um evento imutável. Repetir a mesma chave e comando devolve o evento existente; conteúdo diferente retorna 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento criado ou recuperado pela chave de idempotência", content = @Content(schema = @Schema(implementation = Event.class))),
            @ApiResponse(responseCode = "400", description = "Payload, substituição ou horário inválido"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Refeição de outro paciente"),
            @ApiResponse(responseCode = "404", description = "Refeição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Chave de idempotência usada com outro comando")
    })
    ResponseEntity<Event> create(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID plannedMealId,
            @Parameter(description = "Chave única por paciente", required = true) @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateCompletionEventRequest request
    ) {
        var choices = request.substitutions() == null ? List.<ChoiceRequest>of() : request.substitutions();
        return ResponseEntity.status(201).body(
                operations.create(
                        new Create(
                                actor(jwt), plannedMealId,
                                key, request.eventType(), request.reason(), request.occurredAt(),
                                choices.stream().map(ChoiceRequest::toChoice).toList()
                        )
                )
        );
    }

    @GetMapping("/planned-meals/{plannedMealId}/completion-events")
    @Operation(summary = "Histórico da refeição", description = "Eventos em ordem de criação decrescente, com snapshots originais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de eventos da refeição"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Refeição de outro paciente"),
            @ApiResponse(responseCode = "404", description = "Refeição não encontrada")
    })
    PageResult<Event> meal(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID plannedMealId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return operations.mealEvents(actor(jwt), plannedMealId, page, size);
    }

    @GetMapping("/patients/me/completion-events")
    @Operation(summary = "Histórico do paciente", description = "Eventos de todas as refeições em ordem de criação decrescente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de eventos do paciente"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Perfil de paciente necessário")
    })
    PageResult<Event> patient(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return operations.patientEvents(actor(jwt), page, size);
    }

    @GetMapping("/nutritionists/me/patients/{patientId}/weekly-plans/{planId}/summary")
    @Operation(summary = "Resumo de realização do plano", description = "Inclui refeições atuais e refeições removidas que tenham eventos. Conta o último evento criado por refeição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo de realização", content = @Content(schema = @Schema(implementation = Summary.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Vínculo ativo necessário"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    Summary summary(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId,
            @PathVariable UUID planId
    ) {
        return operations.summary(actor(jwt), patientId, planId);
    }

    private Actor actor(Jwt jwt) {
        return new Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

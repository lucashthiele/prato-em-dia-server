package com.ulvratech.pratoemdia.server.features.planning.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PreferenceUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanningPreferenceOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningCommands;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningQueries;
import com.ulvratech.pratoemdia.server.features.planning.presentation.web.request.WeeklyPlanningRequests.PlannedMealRequest;
import com.ulvratech.pratoemdia.server.features.planning.presentation.web.request.WeeklyPlanningRequests.PreferenceRequest;
import com.ulvratech.pratoemdia.server.features.planning.presentation.web.request.WeeklyPlanningRequests.UpdatePlanRequest;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Planejamento semanal")
public class WeeklyPlanningController {
    private final PlanningCommands commands;
    private final PlanningQueries queries;

    public WeeklyPlanningController(PlanningCommands commands, PlanningQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/patients/me/weekly-planning-preference")
    @Operation(summary = "Consultar preferência de geração", description = "Retorna o dia escolhido pelo paciente para geração automática.")
    WeeklyPlanningPreferenceOutput preference(@AuthenticationPrincipal Jwt jwt) {
        return queries.preference(actor(jwt));
    }

    @PutMapping("/patients/me/weekly-planning-preference")
    @Operation(summary = "Atualizar preferência de geração", description = "Define o dia semanal e ativa ou desativa a geração automática.")
    WeeklyPlanningPreferenceOutput updatePreference(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreferenceRequest request
    ) {
        return commands.updatePreference(
                new PreferenceUpdate(actor(jwt), request.dayOfWeek(), request.enabled())
        );
    }

    @PostMapping("/patients/me/weekly-plans")
    @Operation(summary = "Gerar plano semanal", description = "Gera manualmente o plano para os sete dias seguintes.")
    ResponseEntity<WeeklyPlanOutput> generate(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commands.generate(actor(jwt)));
    }

    @GetMapping("/patients/me/weekly-plans")
    @Operation(summary = "Listar planos semanais", description = "Lista os planos semanais do paciente autenticado.")
    List<WeeklyPlanOutput> list(@AuthenticationPrincipal Jwt jwt) {
        return queries.list(actor(jwt));
    }

    @GetMapping("/weekly-plans/{planId}")
    @Operation(summary = "Consultar plano semanal", description = "Retorna o plano semanal quando ele pertence ao paciente autenticado.")
    WeeklyPlanOutput get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        return queries.get(new PlanQuery(actor(jwt), planId));
    }

    @GetMapping("/nutritionists/me/patients/{patientId}/weekly-plans/{planId}")
    @Operation(summary = "Consultar plano do paciente vinculado", description = "Exige vínculo ativo entre nutricionista e paciente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano semanal do paciente", content = @Content(schema = @Schema(implementation = WeeklyPlanOutput.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Vínculo ativo necessário"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    WeeklyPlanOutput getForNutritionist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID patientId, @PathVariable UUID planId
    ) {
        return queries.getForNutritionist(actor(jwt), patientId, planId);
    }

    @PatchMapping("/weekly-plans/{planId}")
    @Operation(summary = "Alterar plano semanal", description = "Substitui refeições e itens conforme a política copiada para o plano.")
    WeeklyPlanOutput update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID planId,
            @Valid @RequestBody UpdatePlanRequest request
    ) {
        return commands.update(
                new PlanUpdate(
                        actor(jwt),
                        planId,
                        request.version(),
                        request.meals().stream().map(PlannedMealRequest::toInput).toList()
                )
        );
    }

    @PostMapping("/weekly-plans/{planId}/confirm")
    @Operation(summary = "Confirmar plano semanal", description = "Confirma o plano de forma idempotente e bloqueia sua agenda.")
    WeeklyPlanOutput confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        return commands.confirm(new PlanQuery(actor(jwt), planId));
    }

    @DeleteMapping("/weekly-plans/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir plano semanal", description = "Desativa o plano do paciente autenticado (exclusão lógica). O plano deixa de ser listado ou alterado e o período fica livre para um novo plano; os registros de refeições são mantidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano desativado"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Plano de outro paciente"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId) {
        commands.delete(new PlanQuery(actor(jwt), planId));
    }

    private Actor actor(Jwt jwt) {
        return new Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

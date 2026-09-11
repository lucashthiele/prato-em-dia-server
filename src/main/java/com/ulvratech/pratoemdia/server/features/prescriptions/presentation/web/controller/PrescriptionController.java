package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionOutput;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request.PrescriptionContentRequest;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Prescrições", description = "Criação, consulta e versionamento de prescrições alimentares.")
public class PrescriptionController {
    private final PrescriptionCommands commands;
    private final PrescriptionQueries queries;

    public PrescriptionController(PrescriptionCommands commands, PrescriptionQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/nutritionists/me/patients/{patientId}/prescriptions")
    @Operation(
        summary = "Listar prescrições do paciente",
        description = "Retorna as prescrições do paciente vinculado ao nutricionista autenticado."
    )
    List<PrescriptionOutput> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        return queries.list(new PatientQuery(actor(jwt), patientId));
    }

    @PostMapping("/nutritionists/me/patients/{patientId}/prescriptions")
    @Operation(
        summary = "Criar prescrição",
        description = "Cria uma prescrição alimentar inicial para o paciente informado."
    )
    ResponseEntity<PrescriptionOutput> create(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID patientId,
                                               @Valid @RequestBody PrescriptionContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commands.create(new Create(actor(jwt), patientId, request.toInput())));
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    @Operation(
        summary = "Consultar prescrição",
        description = "Retorna uma prescrição acessível ao usuário autenticado."
    )
    PrescriptionOutput get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return queries.get(new PrescriptionQuery(actor(jwt), prescriptionId));
    }

    @PostMapping("/prescriptions/{prescriptionId}/versions")
    @Operation(
        summary = "Criar versão da prescrição",
        description = "Cria um novo rascunho baseado na prescrição informada."
    )
    ResponseEntity<PrescriptionOutput.VersionOutput> createVersion(
        @AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commands.createVersion(new PrescriptionQuery(actor(jwt), prescriptionId)));
    }

    @GetMapping("/prescriptions/{prescriptionId}/versions")
    @Operation(
        summary = "Listar versões da prescrição",
        description = "Retorna o histórico de versões da prescrição informada."
    )
    List<PrescriptionOutput.VersionOutput> versions(
        @AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return queries.listVersions(new PrescriptionQuery(actor(jwt), prescriptionId));
    }

    @GetMapping("/prescription-versions/{versionId}")
    @Operation(
        summary = "Consultar versão da prescrição",
        description = "Retorna o conteúdo de uma versão específica da prescrição."
    )
    PrescriptionOutput.VersionOutput version(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID versionId) {
        return queries.getVersion(new VersionQuery(actor(jwt), versionId));
    }

    @PutMapping("/prescription-versions/{versionId}")
    @Operation(
        summary = "Atualizar versão da prescrição",
        description = "Substitui o conteúdo de uma versão em rascunho."
    )
    PrescriptionOutput.VersionOutput update(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID versionId,
                                             @Valid @RequestBody PrescriptionContentRequest request) {
        return commands.updateVersion(new UpdateVersion(actor(jwt), versionId, request.toInput()));
    }

    @PostMapping("/prescription-versions/{versionId}/publish")
    @Operation(
        summary = "Publicar versão da prescrição",
        description = "Publica uma versão em rascunho e a torna vigente para o paciente."
    )
    PrescriptionOutput.VersionOutput publish(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID versionId) {
        return commands.publish(new VersionQuery(actor(jwt), versionId));
    }

    @PostMapping("/prescription-versions/{versionId}/archive")
    @Operation(
        summary = "Arquivar versão da prescrição",
        description = "Arquiva uma versão publicada da prescrição."
    )
    PrescriptionOutput.VersionOutput archive(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID versionId) {
        return commands.archive(new VersionQuery(actor(jwt), versionId));
    }

    @GetMapping("/patients/me/prescriptions/current")
    @Operation(
        summary = "Consultar prescrição atual",
        description = "Retorna a versão vigente da prescrição do paciente autenticado."
    )
    PrescriptionOutput.VersionOutput current(@AuthenticationPrincipal Jwt jwt) {
        return queries.current(actor(jwt));
    }

    private Actor actor(Jwt jwt) {
        return new Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

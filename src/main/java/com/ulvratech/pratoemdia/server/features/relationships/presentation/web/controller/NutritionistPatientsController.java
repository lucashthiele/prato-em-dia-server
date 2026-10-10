package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.CreateRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndNutritionistRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetNutritionistPatient;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ListNutritionistPatients;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ReissuePatientAccessEmail;
import com.ulvratech.pratoemdia.server.features.relationships.presentation.web.request.CreatePatientRelationshipRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/nutritionists/me/patients")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Pacientes do nutricionista", description = "Gerenciamento dos vínculos entre o nutricionista autenticado e seus pacientes.")
public class NutritionistPatientsController {
    private final CreateRelationship create;
    private final ListNutritionistPatients list;
    private final GetNutritionistPatient get;
    private final EndNutritionistRelationship end;
    private final ReissuePatientAccessEmail reissueAccess;

    public NutritionistPatientsController(
            CreateRelationship c,
            ListNutritionistPatients l,
            GetNutritionistPatient g,
            EndNutritionistRelationship e,
            ReissuePatientAccessEmail r
    ) {
        create = c;
        list = l;
        get = g;
        end = e;
        reissueAccess = r;
    }

    @PostMapping
    @Operation(summary = "Adicionar paciente", description = "Cria um vínculo com um paciente e envia suas instruções de acesso.")
    ResponseEntity<RelationshipOutput> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePatientRelationshipRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(create.execute(new CreateRelationshipInput(id(jwt), request.email(), request.fullName())));
    }

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Retorna uma página dos pacientes vinculados ao nutricionista.")
    PageResult<RelationshipOutput> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return list.execute(new ListRelationshipsInput(id(jwt), page, size));
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "Consultar paciente", description = "Retorna os detalhes do vínculo com o paciente informado.")
    RelationshipOutput get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        return get.execute(new RelationshipQueryInput(id(jwt), patientId));
    }

    @DeleteMapping("/{patientId}/relationship")
    @Operation(summary = "Encerrar vínculo com paciente", description = "Encerra o vínculo ativo entre o nutricionista e o paciente.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void end(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        end.execute(new RelationshipQueryInput(id(jwt), patientId));
    }

    @PostMapping("/{patientId}/access-email")
    @Operation(summary = "Reenviar acesso ao paciente", description = "Agenda o envio das instruções de acesso ao paciente.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void reissueAccess(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        reissueAccess.execute(new RelationshipQueryInput(id(jwt), patientId));
    }

    private UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

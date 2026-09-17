package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetPatientRelationship;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/me/relationship")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Vínculo do paciente", description = "Vínculo profissional do paciente autenticado.")
public class PatientRelationshipController {
    private final GetPatientRelationship get;
    private final EndPatientRelationship end;

    public PatientRelationshipController(
            GetPatientRelationship getPatientRelationship,
            EndPatientRelationship endPatientRelationship
    ) {
        get = getPatientRelationship;
        end = endPatientRelationship;
    }

    @GetMapping
    @Operation(summary = "Consultar vínculo atual", description = "Retorna o vínculo ativo do paciente com seu nutricionista.")
    RelationshipOutput get(@AuthenticationPrincipal Jwt jwt) {
        return get.execute(id(jwt));
    }

    @DeleteMapping
    @Operation(summary = "Encerrar vínculo atual", description = "Encerra o vínculo ativo do paciente com seu nutricionista.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void end(@AuthenticationPrincipal Jwt jwt) {
        end.execute(id(jwt));
    }

    private UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

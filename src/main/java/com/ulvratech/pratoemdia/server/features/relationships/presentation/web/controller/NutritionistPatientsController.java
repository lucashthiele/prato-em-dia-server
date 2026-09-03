package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.relationships.presentation.web.request.CreatePatientRelationshipRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/nutritionists/me/patients")
public class NutritionistPatientsController {
    private final CreateRelationship create;
    private final ListNutritionistPatients list;
    private final GetNutritionistPatient get;
    private final EndNutritionistRelationship end;
    private final ResendPatientAccessEmail resend;

    public NutritionistPatientsController(
        CreateRelationship c,
        ListNutritionistPatients l,
        GetNutritionistPatient g,
        EndNutritionistRelationship e,
        ResendPatientAccessEmail r) {
        create = c;
        list = l;
        get = g;
        end = e;
        resend = r;
    }

    @PostMapping
    ResponseEntity<RelationshipOutput> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreatePatientRelationshipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(create.execute(new CreateRelationshipInput(id(jwt), request.email(), request.fullName())));
    }

    @GetMapping
    PageResult<RelationshipOutput> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return list.execute(new ListRelationshipsInput(id(jwt), page, size));
    }

    @GetMapping("/{patientId}")
    RelationshipOutput get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        return get.execute(new RelationshipQueryInput(id(jwt), patientId));
    }

    @DeleteMapping("/{patientId}/relationship")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void end(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        end.execute(new RelationshipQueryInput(id(jwt), patientId));
    }

    @PostMapping("/{patientId}/access-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        resend.execute(new RelationshipQueryInput(id(jwt), patientId));
    }

    private UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

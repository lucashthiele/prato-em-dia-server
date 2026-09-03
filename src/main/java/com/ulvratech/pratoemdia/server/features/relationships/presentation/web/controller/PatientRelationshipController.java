package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetPatientRelationship;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/me/relationship")
public class PatientRelationshipController {
    private final GetPatientRelationship get;
    private final EndPatientRelationship end;

    public PatientRelationshipController(GetPatientRelationship getPatientRelationship, EndPatientRelationship endPatientRelationship) {
        get = getPatientRelationship;
        end = endPatientRelationship;
    }

    @GetMapping
    RelationshipOutput get(@AuthenticationPrincipal Jwt jwt) {
        return get.execute(id(jwt));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void end(@AuthenticationPrincipal Jwt jwt) {
        end.execute(id(jwt));
    }

    private UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

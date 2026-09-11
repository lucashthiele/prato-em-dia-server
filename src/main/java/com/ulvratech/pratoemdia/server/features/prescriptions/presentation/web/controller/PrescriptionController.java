package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionOutput;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request.PrescriptionContentRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PrescriptionController {
    private final PrescriptionCommands commands;
    private final PrescriptionQueries queries;

    public PrescriptionController(PrescriptionCommands commands, PrescriptionQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/nutritionists/me/patients/{patientId}/prescriptions")
    List<PrescriptionOutput> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID patientId) {
        return queries.list(new PatientQuery(actor(jwt), patientId));
    }

    @PostMapping("/nutritionists/me/patients/{patientId}/prescriptions")
    ResponseEntity<PrescriptionOutput> create(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID patientId,
                                               @Valid @RequestBody PrescriptionContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commands.create(new Create(actor(jwt), patientId, request.toInput())));
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    PrescriptionOutput get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return queries.get(new PrescriptionQuery(actor(jwt), prescriptionId));
    }

    @PostMapping("/prescriptions/{prescriptionId}/versions")
    ResponseEntity<PrescriptionOutput.VersionOutput> createVersion(
        @AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commands.createVersion(new PrescriptionQuery(actor(jwt), prescriptionId)));
    }

    @GetMapping("/prescriptions/{prescriptionId}/versions")
    List<PrescriptionOutput.VersionOutput> versions(
        @AuthenticationPrincipal Jwt jwt, @PathVariable UUID prescriptionId) {
        return queries.listVersions(new PrescriptionQuery(actor(jwt), prescriptionId));
    }

    @GetMapping("/prescription-versions/{versionId}")
    PrescriptionOutput.VersionOutput version(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID versionId) {
        return queries.getVersion(new VersionQuery(actor(jwt), versionId));
    }

    @PutMapping("/prescription-versions/{versionId}")
    PrescriptionOutput.VersionOutput update(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID versionId,
                                             @Valid @RequestBody PrescriptionContentRequest request) {
        return commands.updateVersion(new UpdateVersion(actor(jwt), versionId, request.toInput()));
    }

    @PostMapping("/prescription-versions/{versionId}/publish")
    PrescriptionOutput.VersionOutput publish(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID versionId) {
        return commands.publish(new VersionQuery(actor(jwt), versionId));
    }

    @PostMapping("/prescription-versions/{versionId}/archive")
    PrescriptionOutput.VersionOutput archive(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID versionId) {
        return commands.archive(new VersionQuery(actor(jwt), versionId));
    }

    @GetMapping("/patients/me/prescriptions/current")
    PrescriptionOutput.VersionOutput current(@AuthenticationPrincipal Jwt jwt) {
        return queries.current(actor(jwt));
    }

    private Actor actor(Jwt jwt) {
        return new Actor(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"));
    }
}

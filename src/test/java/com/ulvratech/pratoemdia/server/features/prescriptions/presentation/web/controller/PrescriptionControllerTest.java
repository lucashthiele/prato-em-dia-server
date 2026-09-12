package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionContentInput;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request.PrescriptionContentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrescriptionControllerTest {
    @Test
    void delegatesEveryPrescriptionEndpoint() {
        var commands = mock(PrescriptionCommands.class); var queries = mock(PrescriptionQueries.class);
        when(queries.list(any())).thenReturn(List.of());
        when(queries.listVersions(any())).thenReturn(List.of());
        var controller = new PrescriptionController(commands, queries);
        var substitution = new PrescriptionContentRequest.SubstitutionRequest(
            UUID.randomUUID(), null, BigDecimal.ONE, "g", null);
        var item = new PrescriptionContentRequest.ItemRequest(UUID.randomUUID(), null,
            BigDecimal.ONE, "g", 0, null, List.of(substitution));
        var meal = new PrescriptionContentRequest.MealRequest("Meal", 0, null, null, List.of(item));
        var request = new PrescriptionContentRequest("Plan", null, List.of(meal));
        var jwt = jwt(UUID.randomUUID(), "NUTRITIONIST");
        var patient = UUID.randomUUID(); var prescription = UUID.randomUUID(); var version = UUID.randomUUID();

        assertTrue(controller.list(jwt, patient).isEmpty());
        assertEquals(201, controller.create(jwt, patient, request).getStatusCode().value());
        controller.get(jwt, prescription);
        assertEquals(201, controller.createVersion(jwt, prescription).getStatusCode().value());
        assertTrue(controller.versions(jwt, prescription).isEmpty());
        controller.version(jwt, version);
        controller.update(jwt, version, request);
        controller.publish(jwt, version);
        controller.archive(jwt, version);
        controller.current(jwt);

        verify(commands).create(any(Create.class));
        verify(commands).createVersion(any(PrescriptionQuery.class));
        verify(commands).updateVersion(any(UpdateVersion.class));
        verify(commands).publish(any(VersionQuery.class));
        verify(commands).archive(any(VersionQuery.class));
        verify(queries).get(any(PrescriptionQuery.class));
        verify(queries).getVersion(any(VersionQuery.class));
        verify(queries).current(any(Actor.class));
    }

    private static Jwt jwt(UUID id, String role) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(id.toString())
            .claim("role", role).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
            .build();
    }
}

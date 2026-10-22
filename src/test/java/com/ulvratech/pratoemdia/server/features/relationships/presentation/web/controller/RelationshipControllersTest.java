package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.CreateRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndNutritionistRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetNutritionistPatient;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ListNutritionistPatients;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ReissuePatientAccessEmail;
import com.ulvratech.pratoemdia.server.features.relationships.presentation.web.request.CreatePatientRelationshipRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RelationshipControllersTest {
    @Test
    void nutritionistControllerDelegatesEveryOperation() {
        var create = mock(CreateRelationship.class);
        var list = mock(ListNutritionistPatients.class);
        var get = mock(GetNutritionistPatient.class);
        var end = mock(EndNutritionistRelationship.class);
        var reissueAccess = mock(ReissuePatientAccessEmail.class);
        var controller = new NutritionistPatientsController(create, list, get, end, reissueAccess);
        var actor = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var jwt = jwt(actor);

        assertEquals(
                201, controller.create(
                        jwt,
                        new CreatePatientRelationshipRequest("patient@example.com", "Patient")
                )
                        .getStatusCode().value()
        );
        controller.list(jwt, 2, 15, null, null, null, null);
        controller.get(jwt, patient);
        controller.end(jwt, patient);
        controller.reissueAccess(jwt, patient);

        verify(create).execute(any(CreateRelationshipInput.class));
        verify(list).execute(new ListRelationshipsInput(actor, 2, 15));
        verify(get).execute(new RelationshipQueryInput(actor, patient));
        verify(end).execute(new RelationshipQueryInput(actor, patient));
        verify(reissueAccess).execute(new RelationshipQueryInput(actor, patient));
    }

    @Test
    void patientControllerDelegatesGetAndEnd() {
        var get = mock(GetPatientRelationship.class);
        var end = mock(EndPatientRelationship.class);
        var controller = new PatientRelationshipController(get, end);
        var id = UUID.randomUUID();
        var jwt = jwt(id);

        controller.get(jwt);
        controller.end(jwt);

        verify(get).execute(id);
        verify(end).execute(id);
    }

    private static Jwt jwt(UUID id) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(id.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}

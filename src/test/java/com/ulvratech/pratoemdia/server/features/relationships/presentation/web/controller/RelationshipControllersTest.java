package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.relationships.presentation.web.request.CreatePatientRelationshipRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class RelationshipControllersTest {
  private static Jwt jwt(UUID id) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(id.toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }

  @Test
  void nutritionistControllerDelegatesEveryOperation() {
    var create = mock(CreateRelationship.class);
    var list = mock(ListNutritionistPatients.class);
    var get = mock(GetNutritionistPatient.class);
    var end = mock(EndNutritionistRelationship.class);
    var resend = mock(ResendPatientAccessEmail.class);
    var controller = new NutritionistPatientsController(create, list, get, end, resend);
    var actor = UUID.randomUUID();
    var patient = UUID.randomUUID();
    var jwt = jwt(actor);

    assertEquals(
        201,
        controller
            .create(jwt, new CreatePatientRelationshipRequest("patient@example.com", "Patient"))
            .getStatusCode()
            .value());
    controller.list(jwt, 2, 15);
    controller.get(jwt, patient);
    controller.end(jwt, patient);
    controller.resend(jwt, patient);

    verify(create).execute(any(CreateRelationshipInput.class));
    verify(list).execute(new ListRelationshipsInput(actor, 2, 15));
    verify(get).execute(new RelationshipQueryInput(actor, patient));
    verify(end).execute(new RelationshipQueryInput(actor, patient));
    verify(resend).execute(new RelationshipQueryInput(actor, patient));
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
}

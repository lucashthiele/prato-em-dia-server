package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.model.RelationshipModel;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RelationshipMapperTest {
  @Test
  void mapsRelationshipInBothDirections() {
    var now = Instant.now();
    var id = UUID.randomUUID();
    var model =
        RelationshipModel.builder()
            .id(id)
            .nutritionistId(UUID.randomUUID())
            .patientId(UUID.randomUUID())
            .status(RelationshipStatus.ENDED)
            .endedAt(now)
            .endedByUserAccountId(UUID.randomUUID())
            .emailDeliveryStatus(RelationshipEmailStatus.SENT)
            .emailAttemptCount(2)
            .emailLastAttemptAt(now)
            .createdAt(now)
            .updatedAt(now)
            .version(4)
            .build();
    var mapper = new RelationshipMapper();

    ProfessionalPatientRelationship domain = mapper.toDomain(model);
    RelationshipModel roundTrip = mapper.toModel(domain);

    assertEquals(id, roundTrip.getId());
    assertEquals(RelationshipStatus.ENDED, roundTrip.getStatus());
    assertEquals(RelationshipEmailStatus.SENT, roundTrip.getEmailDeliveryStatus());
    assertEquals(2, roundTrip.getEmailAttemptCount());
    assertEquals(4, roundTrip.getVersion());
    assertEquals(now, roundTrip.getEndedAt());
  }
}

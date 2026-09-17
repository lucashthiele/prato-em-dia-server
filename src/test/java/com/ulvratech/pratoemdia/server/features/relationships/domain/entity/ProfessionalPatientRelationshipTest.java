package com.ulvratech.pratoemdia.server.features.relationships.domain.entity;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfessionalPatientRelationshipTest {
    @Test
    void keepsEndAndEmailAudit() {
        var relationship = ProfessionalPatientRelationship.create(UUID.randomUUID(), UUID.randomUUID());
        var actor = UUID.randomUUID();
        var now = Instant.parse("2026-09-01T12:00:00Z");
        relationship.recordEmail(RelationshipEmailStatus.FAILED, now);
        relationship.end(actor, now);
        assertEquals(RelationshipStatus.ENDED, relationship.getStatus());
        assertEquals(actor, relationship.getEndedByUserAccountId());
        assertEquals(1, relationship.getEmailAttemptCount());
        assertEquals(RelationshipEmailStatus.FAILED, relationship.getEmailDeliveryStatus());
        assertThrows(IllegalStateException.class, () -> relationship.end(actor, now));
    }
}

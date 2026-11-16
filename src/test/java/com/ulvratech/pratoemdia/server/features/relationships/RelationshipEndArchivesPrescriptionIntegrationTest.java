package com.ulvratech.pratoemdia.server.features.relationships;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter.PrescriptionQueryAdapter;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndNutritionistRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndPatientRelationship;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class RelationshipEndArchivesPrescriptionIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PrescriptionRepository prescriptions;
    @Autowired
    PrescriptionQueryAdapter query;
    @Autowired
    EndNutritionistRelationship endAsNutritionist;
    @Autowired
    EndPatientRelationship endAsPatient;

    @Test
    void endingAsNutritionistArchivesPrescriptionAndPublishedVersionSoThePatientHasNoCurrentVersion() {
        var pair = seedPair();
        var prescription = publishedWithOpenDraft(pair);
        assertTrue(query.currentPublishedVersion(pair.patient()).isPresent());

        endAsNutritionist.execute(new RelationshipQueryInput(pair.nutritionistAccount(), pair.patient()));

        assertArchived(prescription.getId());
        assertTrue(query.currentPublishedVersion(pair.patient()).isEmpty());
    }

    @Test
    void endingAsPatientArchivesPrescriptionToo() {
        var pair = seedPair();
        var prescription = publishedWithOpenDraft(pair);

        endAsPatient.execute(pair.patientAccount());

        assertArchived(prescription.getId());
        assertTrue(query.currentPublishedVersion(pair.patient()).isEmpty());
    }

    private void assertArchived(UUID prescriptionId) {
        var reloaded = prescriptions.findById(prescriptionId).orElseThrow();
        assertEquals(PrescriptionStatus.ARCHIVED, reloaded.getStatus());
        for (var version : reloaded.getVersions()) {
            assertEquals(PrescriptionVersionStatus.ARCHIVED, version.getStatus());
            assertNotNull(version.getArchivedAt());
        }
        assertNotNull(
                reloaded.getVersions().stream().filter(v -> v.getNumber() == 1).findFirst().orElseThrow()
                        .getPublishedAt()
        );
        assertNull(
                reloaded.getVersions().stream().filter(v -> v.getArchivedAt() == null).findFirst().orElse(null)
        );
    }

    private Prescription publishedWithOpenDraft(Pair pair) {
        var meal = PrescribedMeal.create(
                "Lunch", 0, null, null, List.of(
                        PrescribedMealItem.create(pair.food(), null, new BigDecimal("100"), "g", 0, null, List.of())
                )
        );
        var saved = prescriptions.save(
                Prescription.create(
                        pair.patient(), pair.nutritionist(), pair.nutritionistAccount(), "Plan", null, List.of(meal)
                )
        );
        saved.publish(saved.getVersions().getFirst().getId(), Instant.parse("2026-11-01T12:00:00Z"));
        var published = prescriptions.save(saved);
        published.addDraft(pair.nutritionistAccount());
        return prescriptions.save(published);
    }

    private Pair seedPair() {
        var suffix = UUID.randomUUID().toString();
        var pair = new Pair(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
                pair.nutritionistAccount(), "n-" + suffix + "@example.com", "hash", "NUTRITIONIST"
        );
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
                pair.patientAccount(), "p-" + suffix + "@example.com", "hash", "PATIENT"
        );
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
                pair.nutritionist(), pair.nutritionistAccount(), "Nutri"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                pair.patient(), pair.patientAccount(), "Paciente"
        );
        jdbc.update(
                "insert into food(id,name,default_unit,created_by_user_id,origin) values (?,?,?,?,?::food_origin)",
                pair.food(), "Arroz " + suffix, "g", pair.nutritionistAccount(), "NUTRITIONIST"
        );
        jdbc.update(
                "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id) values (?,?,?)",
                UUID.randomUUID(), pair.nutritionist(), pair.patient()
        );
        return pair;
    }

    private record Pair(UUID nutritionistAccount, UUID patientAccount, UUID nutritionist, UUID patient, UUID food) {
    }
}

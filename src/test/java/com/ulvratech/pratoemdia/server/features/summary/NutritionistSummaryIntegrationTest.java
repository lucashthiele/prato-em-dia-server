package com.ulvratech.pratoemdia.server.features.summary;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class NutritionistSummaryIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void summarizesOnlyActiveRelationshipsOfTheAuthenticatedNutritionist() throws Exception {
        var account = account("NUTRITIONIST");
        var nutritionist = nutritionist(account, "Nutri");
        var otherAccount = account("NUTRITIONIST");
        var other = nutritionist(otherAccount, "Other");

        var sent = patient(nutritionist, "Ana", "SENT", "ACTIVE");
        var pending = patient(nutritionist, "Bia", "PENDING", "ACTIVE");
        var failed = patient(nutritionist, "Caio", "FAILED", "ACTIVE");
        var ended = patient(nutritionist, "Davi", "FAILED", "ENDED");
        var othersPatient = patient(other, "Eva", "PENDING", "ACTIVE");

        var older = draft(nutritionist, sent, 1, "Older", "2026-01-01T10:00:00Z");
        var newer = draft(nutritionist, pending, 3, "Newer", "2026-02-01T10:00:00Z");
        draft(nutritionist, ended, 1, "Ended patient", "2026-03-01T10:00:00Z");
        draft(other, othersPatient, 1, "Other nutritionist", "2026-03-01T10:00:00Z");
        publishedVersion(nutritionist, failed);

        mvc.perform(get("/api/v1/nutritionists/me/summary").with(auth(account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.activePatients").value(3))
                .andExpect(jsonPath("$.counts.pendingAccess").value(1))
                .andExpect(jsonPath("$.counts.failedAccess").value(1))
                .andExpect(jsonPath("$.counts.openDrafts").value(2))
                .andExpect(jsonPath("$.drafts.length()").value(2))
                .andExpect(jsonPath("$.drafts[0].versionId").value(newer.version().toString()))
                .andExpect(jsonPath("$.drafts[0].patientFullName").value("Bia"))
                .andExpect(jsonPath("$.drafts[0].versionNumber").value(3))
                .andExpect(jsonPath("$.drafts[0].prescriptionId").value(newer.prescription().toString()))
                .andExpect(jsonPath("$.drafts[1].versionId").value(older.version().toString()));

        mvc.perform(get("/api/v1/nutritionists/me/summary?draftLimit=1").with(auth(account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.openDrafts").value(2))
                .andExpect(jsonPath("$.drafts.length()").value(1));
        mvc.perform(get("/api/v1/nutritionists/me/summary?draftLimit=0").with(auth(account)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void returnsZerosWhenNutritionistHasNoPatients() throws Exception {
        var account = account("NUTRITIONIST");
        nutritionist(account, "Empty");

        mvc.perform(get("/api/v1/nutritionists/me/summary").with(auth(account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.activePatients").value(0))
                .andExpect(jsonPath("$.counts.openDrafts").value(0))
                .andExpect(jsonPath("$.drafts.length()").value(0));
    }

    private record Draft(UUID prescription, UUID version) {
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor auth(UUID account) {
        return jwt().jwt(token -> token.subject(account.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"));
    }

    private UUID account(String role) {
        var id = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
                id, id + "@example.com", "hash", role
        );
        return id;
    }

    private UUID nutritionist(UUID account, String name) {
        var id = UUID.randomUUID();
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
                id, account, name
        );
        return id;
    }

    private UUID patient(UUID nutritionist, String name, String emailStatus, String relationshipStatus) {
        var account = account("PATIENT");
        var id = UUID.randomUUID();
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                id, account, name
        );
        var ended = relationshipStatus.equals("ENDED");
        jdbc.update(
                "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id,status,"
                        + "email_delivery_status,ended_at,ended_by_user_account_id)"
                        + " values (?,?,?,?::relationship_status,?::relationship_email_status,?,?)",
                UUID.randomUUID(), nutritionist, id, relationshipStatus, emailStatus,
                ended ? java.sql.Timestamp.from(Instant.now()) : null, ended ? account : null
        );
        return id;
    }

    private Draft draft(UUID nutritionist, UUID patient, int number, String title, String updatedAt) {
        var prescription = prescription(nutritionist, patient);
        var version = UUID.randomUUID();
        insertVersion(prescription, version, number, "DRAFT", title, updatedAt);
        return new Draft(prescription, version);
    }

    private void publishedVersion(UUID nutritionist, UUID patient) {
        insertVersion(
                prescription(nutritionist, patient), UUID.randomUUID(), 1, "PUBLISHED", "Published",
                "2026-04-01T10:00:00Z"
        );
    }

    private UUID prescription(UUID nutritionist, UUID patient) {
        var id = UUID.randomUUID();
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)", id, patient, nutritionist
        );
        return id;
    }

    private void insertVersion(UUID prescription, UUID id, int number, String status, String title, String updatedAt) {
        var author = jdbc.queryForObject("select user_account_id from nutritionist_profile limit 1", UUID.class);
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,title,"
                        + "created_by_user_id,published_at,updated_at)"
                        + " values (?,?,?,?::prescription_version_status,?,?,?,?)",
                id, prescription, number, status, title, author,
                status.equals("PUBLISHED") ? java.sql.Timestamp.from(Instant.now()) : null,
                java.sql.Timestamp.from(Instant.parse(updatedAt))
        );
    }
}

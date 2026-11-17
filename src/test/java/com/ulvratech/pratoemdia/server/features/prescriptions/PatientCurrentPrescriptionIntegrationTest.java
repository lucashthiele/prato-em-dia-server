package com.ulvratech.pratoemdia.server.features.prescriptions;

import com.ulvratech.pratoemdia.server.support.TrackingFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roda a consulta da prescrição atual no PostgreSQL real (P-04: o DISTINCT com ORDER BY quebrava ali). */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "consent.enforced=false"
})
@AutoConfigureMockMvc
class PatientCurrentPrescriptionIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MockMvc mvc;

    ResultActions current(TrackingFixture f) throws Exception {
        return mvc.perform(
                get("/api/v1/patients/me/prescriptions/current")
                        .with(
                                jwt().jwt(t -> t.subject(f.patientAccount.toString()).claim("role", "PATIENT"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                        )
        );
    }

    @Test
    void returnsThePublishedVersion() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        f.prescribedMeal("LUNCH", f.food("Rice"));
        f.publish();

        current(f).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(f.version.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.meals[0].mealType").value("LUNCH"));
    }

    @Test
    void answersNotFoundWhenThereIsOnlyADraft() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        f.prescribedMeal("LUNCH", f.food("Rice"));

        current(f).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRESCRIPTION_NOT_FOUND"));
    }
}

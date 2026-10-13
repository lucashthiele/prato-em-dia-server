package com.ulvratech.pratoemdia.server.features.tracking;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes;

import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.DiaryOperations;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class DiaryCalendarIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DiaryOperations diary;
    @Autowired
    MockMvc mvc;

    @Test
    void removesTheLegacyCompletionEventTable() {
        assertNull(jdbc.queryForObject("select to_regclass('meal_completion_event')", String.class));
    }

    @Test
    void returnsExplicitPlanPeriodForPatient() {
        var account = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var nutritionistAccount = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var prescription = UUID.randomUUID();
        var version = UUID.randomUUID();
        var plan = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT'::user_role)",
                account, account + "@example.com", "hash"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                patient, account, "Patient"
        );
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'NUTRITIONIST'::user_role)",
                nutritionistAccount, nutritionistAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
                nutritionist, nutritionistAccount, "Nutritionist"
        );
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)",
                prescription, patient, nutritionist
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id,published_at) values (?,?,1,'PUBLISHED',?,now())",
                version, prescription, nutritionistAccount
        );
        jdbc.update(
                "insert into weekly_plan(id,patient_id,prescription_version_id,planning_mode,period_start,period_end,confirmation_deadline) values (?,?,?,'FREE','2026-10-12','2026-10-18',now())",
                plan, patient, version
        );

        var calendar = diary.calendar(new DiaryTypes.Actor(account, "PATIENT"));

        assertEquals(plan, calendar.plans().getFirst().planId());
        assertEquals("2026-10-12", calendar.plans().getFirst().start().toString());
        assertEquals("2026-10-18", calendar.plans().getFirst().end().toString());

        try {
            mvc.perform(
                    get("/api/v1/patients/me/calendar-periods")
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.plans[0].planId").value(plan.toString()));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}

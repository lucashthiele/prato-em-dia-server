package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import com.ulvratech.pratoemdia.server.features.tracking.domain.port.out.MealCompletionRepository;
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

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class TrackingMigrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MealCompletionRepository repository;
    @Autowired
    MockMvc mvc;

    @Test
    void storesSnapshotsOrdersHistoryAndRetainsEventsAfterMealRemoval() throws Exception {
        UUID nutritionistAccount = UUID.randomUUID(), patientAccount = UUID.randomUUID();
        UUID nutritionist = UUID.randomUUID(), patient = UUID.randomUUID();
        UUID prescription = UUID.randomUUID(), version = UUID.randomUUID();
        UUID plan = UUID.randomUUID(), meal = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'NUTRITIONIST')",
                nutritionistAccount, nutritionistAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT')",
                patientAccount, patientAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
                nutritionist, nutritionistAccount, "Nutritionist"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                patient, patientAccount, "Patient"
        );
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)",
                prescription, patient, nutritionist
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,created_by_user_id) "
                        + "values (?,?,1,?)",
                version, prescription, nutritionistAccount
        );
        jdbc.update(
                "insert into weekly_plan(id,patient_id,prescription_version_id,planning_mode,"
                        + "period_start,period_end,confirmation_deadline) values (?,?,?,'FREE',"
                        + "DATE '2026-09-18',DATE '2026-09-24',TIMESTAMPTZ '2026-09-18 03:00:00+00')",
                plan, patient, version
        );
        jdbc.update(
                "insert into planned_meal(id,weekly_plan_id,meal_date,meal_type,name,meal_order) "
                        + "values (?,?,DATE '2026-09-18','LUNCH','Lunch',0)",
                meal, plan
        );

        var first = repository.append(event(patient, plan, meal, "one", Type.COMPLETED));
        var second = repository.append(event(patient, plan, meal, "two", Type.NOT_COMPLETED));
        assertTrue(second.sequenceId() > first.sequenceId());
        assertEquals(first.id(), repository.append(event(patient, plan, meal, "one", Type.COMPLETED)).id());
        assertEquals(second.id(), repository.forMeal(meal, 0, 1).content().getFirst().id());
        assertEquals(2, repository.forPatient(patient, 0, 1).totalElements());
        assertEquals("Lunch", first.mealSnapshot().contains("Lunch") ? "Lunch" : "missing");

        jdbc.update("delete from planned_meal where id=?", meal);
        assertEquals(patient, repository.patientForMeal(meal).orElseThrow());
        assertEquals(2, repository.forPlan(plan).size());
        assertThrows(Exception.class, () -> jdbc.update("delete from meal_completion_event where id=?", first.id()));

        String url = "/api/v1/planned-meals/" + meal + "/completion-events";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(
                get(url).with(
                        jwt().jwt(
                                j -> j.subject(nutritionistAccount.toString())
                                        .claim("role", "NUTRITIONIST")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                )
        )
                .andExpect(status().isForbidden());
        mvc.perform(
                get(url + "?page=0&size=1").with(
                        jwt().jwt(
                                j -> j.subject(patientAccount.toString())
                                        .claim("role", "PATIENT")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
        )
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].mealSnapshot.name").value("Lunch"));
        mvc.perform(
                post(url).with(
                        jwt().jwt(
                                j -> j.subject(patientAccount.toString())
                                        .claim("role", "PATIENT")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
                        .contentType("application/json")
                        .content("{\"eventType\":\"COMPLETED\",\"occurredAt\":\"2026-09-18T15:00:00Z\"}")
        )
                .andExpect(status().isBadRequest());

        UUID otherAccount = UUID.randomUUID(), otherPatient = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT')",
                otherAccount, otherAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                otherPatient, otherAccount, "Other"
        );
        mvc.perform(
                get(url).with(
                        jwt().jwt(
                                j -> j.subject(otherAccount.toString())
                                        .claim("role", "PATIENT")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
        )
                .andExpect(status().isForbidden());

        UUID relationship = UUID.randomUUID();
        jdbc.update(
                "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id) "
                        + "values (?,?,?)",
                relationship, nutritionist, patient
        );
        String professional = "/api/v1/nutritionists/me/patients/" + patient
                + "/weekly-plans/" + plan;
        mvc.perform(
                get(professional).with(
                        jwt().jwt(
                                j -> j.subject(nutritionistAccount.toString())
                                        .claim("role", "NUTRITIONIST")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                )
        )
                .andExpect(status().isOk());
        mvc.perform(
                get(professional + "/summary").with(
                        jwt().jwt(
                                j -> j.subject(nutritionistAccount.toString())
                                        .claim("role", "NUTRITIONIST")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                )
        )
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.notCompleted").value(1));
        jdbc.update(
                "update nutritionist_patient_relationship set status='ENDED', "
                        + "ended_at=current_timestamp,ended_by_user_account_id=? where id=?",
                nutritionistAccount, relationship
        );
        mvc.perform(
                get(professional).with(
                        jwt().jwt(
                                j -> j.subject(nutritionistAccount.toString())
                                        .claim("role", "NUTRITIONIST")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                )
        )
                .andExpect(status().isForbidden());
        mvc.perform(
                get(professional + "/summary").with(
                        jwt().jwt(
                                j -> j.subject(nutritionistAccount.toString())
                                        .claim("role", "NUTRITIONIST")
                        )
                                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                )
        )
                .andExpect(status().isForbidden());
    }

    private MealCompletionEvent event(UUID patient, UUID plan, UUID meal, String key, Type type) {
        return new MealCompletionEvent(
                0, UUID.randomUUID(), patient, plan, meal, type, null,
                Instant.parse("2026-09-18T15:00:00Z"), Instant.now(), key, "hash", "{\"name\":\"Lunch\"}",
                "[]", "[]"
        );
    }
}

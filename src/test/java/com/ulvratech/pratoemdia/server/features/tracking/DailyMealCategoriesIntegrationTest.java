package com.ulvratech.pratoemdia.server.features.tracking;

import java.time.LocalDate;
import java.time.LocalTime;
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
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import static com.ulvratech.pratoemdia.server.features.tracking.TrackingFixture.input;
import static com.ulvratech.pratoemdia.server.features.tracking.TrackingFixture.write;
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
class DailyMealCategoriesIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static final LocalDate DAY = LocalDate.parse("2026-10-13");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ExecutionOperations executions;
    @Autowired
    MockMvc mvc;

    org.springframework.test.web.servlet.ResultActions categories(TrackingFixture f, LocalDate date)
            throws Exception {
        return mvc.perform(
                get("/api/v1/patients/me/daily-meal-categories").param("date", date.toString())
                        .with(
                                jwt().jwt(t -> t.subject(f.patientAccount.toString()).claim("role", "PATIENT"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                        )
        );
    }

    @Test
    void exposesStateAndCoverageOfEachCategoryInRestrictedMode() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        var lunchPrescribed = f.prescribedMeal("LUNCH", f.food("Rice"), f.food("Beans"));
        f.prescribedMeal("DINNER");
        var breakfastPrescribed = f.prescribedMeal("BREAKFAST", f.food("Bread"));
        var lunch = f.plannedMeal(DAY, lunchPrescribed);
        f.plannedMeal(DAY.plusDays(1), breakfastPrescribed);

        categories(f, DAY).andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(DAY.toString()))
                .andExpect(jsonPath("$.categories.length()").value(4))
                .andExpect(jsonPath("$.categories[0].mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.categories[0].state").value("NOT_LOGGED"))
                .andExpect(jsonPath("$.categories[0].canRegister").value(false))
                .andExpect(jsonPath("$.categories[0].blockedReason").value("NO_ALLOWED_FOODS"))
                .andExpect(jsonPath("$.categories[1].mealType").value("LUNCH"))
                .andExpect(jsonPath("$.categories[1].plannedMealId").value(lunch.id().toString()))
                .andExpect(jsonPath("$.categories[1].canRegister").value(true))
                .andExpect(jsonPath("$.categories[1].coveredItemCount").value(0))
                .andExpect(jsonPath("$.categories[1].uncoveredItemCount").value(2));

        executions.create(f.actor(), "k1", write(lunch, LocalTime.NOON, input(lunch.items().get(0))));
        categories(f, DAY)
                .andExpect(jsonPath("$.categories[1].state").value("PARTIAL"))
                .andExpect(jsonPath("$.categories[1].coveredItemCount").value(1))
                .andExpect(jsonPath("$.categories[1].uncoveredItemCount").value(1))
                .andExpect(
                        jsonPath("$.categories[1].uncoveredPlannedItemIds[0]")
                                .value(lunch.items().get(1).id().toString())
                );

        executions.create(f.actor(), "k2", write(lunch, LocalTime.of(13, 0), input(lunch.items().get(1))));
        categories(f, DAY)
                .andExpect(jsonPath("$.categories[1].state").value("LOGGED"))
                .andExpect(jsonPath("$.categories[1].uncoveredItemCount").value(0));
    }

    @Test
    void freeModeAllowsEveryCategoryAndNeverReportsPartial() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");
        var lunch = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice"), f.food("Beans")));
        executions.create(f.actor(), "k1", write(lunch, LocalTime.NOON, input(lunch.items().get(0))));

        categories(f, DAY).andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].canRegister").value(true))
                .andExpect(jsonPath("$.categories[0].blockedReason").doesNotExist())
                .andExpect(jsonPath("$.categories[1].state").value("LOGGED"))
                .andExpect(jsonPath("$.categories[3].canRegister").value(true));
    }

    @Test
    void dayWithoutPlanFollowsThePublishedPrescriptionMode() throws Exception {
        var restricted = new TrackingFixture(jdbc, "RESTRICTED");
        restricted.prescribedMeal("LUNCH", restricted.food("Rice"));
        restricted.publish();
        var free = new TrackingFixture(jdbc, "FREE");
        free.prescribedMeal("LUNCH", free.food("Rice"));
        free.publish();

        categories(restricted, DAY).andExpect(jsonPath("$.categories[1].canRegister").value(false))
                .andExpect(jsonPath("$.categories[1].blockedReason").value("NO_ALLOWED_FOODS"));
        categories(free, DAY.plusDays(30)).andExpect(jsonPath("$.categories[1].canRegister").value(true))
                .andExpect(jsonPath("$.categories[1].state").value("NOT_LOGGED"));
    }

    @Test
    void nutritionistRoleCannotUseThePatientRoute() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");

        mvc.perform(
                get("/api/v1/patients/me/daily-meal-categories").param("date", DAY.toString())
                        .with(
                                jwt().jwt(
                                        t -> t.subject(f.nutritionistAccount.toString()).claim("role", "NUTRITIONIST")
                                )
                                        .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                        )
        ).andExpect(status().isForbidden());
    }
}

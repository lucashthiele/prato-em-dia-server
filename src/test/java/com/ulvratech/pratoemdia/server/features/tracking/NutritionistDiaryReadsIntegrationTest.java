package com.ulvratech.pratoemdia.server.features.tracking;

import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import com.ulvratech.pratoemdia.server.support.TrackingFixture;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.ulvratech.pratoemdia.server.support.TrackingFixture.free;
import static com.ulvratech.pratoemdia.server.support.TrackingFixture.input;
import static com.ulvratech.pratoemdia.server.support.TrackingFixture.write;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class NutritionistDiaryReadsIntegrationTest {
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

    ResultActions asNutritionist(UUID account, String path, String... params) throws Exception {
        return asNutritionist(HttpMethod.GET, account, path, params);
    }

    ResultActions asNutritionist(HttpMethod method, UUID account, String path, String... params)
            throws Exception {
        var request = MockMvcRequestBuilders.request(method, "/api/v1/nutritionists/me/patients/" + path);
        for (int i = 0; i < params.length; i += 2)
            request.param(params[i], params[i + 1]);
        return mvc.perform(
                request.with(
                        jwt().jwt(t -> t.subject(account.toString()).claim("role", "NUTRITIONIST"))
                                .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                )
        );
    }

    @Test
    void linkedNutritionistReadsExecutionsByPeriodAndById() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        var lunch = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice")));
        var first = executions.create(
                f.actor(), "k1", write(lunch, LocalTime.NOON, input(lunch.items().getFirst()))
        );
        var removed = executions.create(
                f.actor(), "k2", write(lunch, LocalTime.of(13, 0), input(lunch.items().getFirst()))
        );
        executions.delete(f.actor(), removed.id(), removed.version(), "k3");

        asNutritionist(
                f.nutritionistAccount, f.patient + "/meal-executions", "from", DAY.minusDays(1).toString(),
                "to", DAY.plusDays(1).toString()
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(first.id().toString()))
                .andExpect(jsonPath("$[0].items[0].kcal").exists());
        asNutritionist(
                f.nutritionistAccount, f.patient + "/meal-executions", "from", DAY.plusDays(1).toString(),
                "to", DAY.plusDays(2).toString()
        ).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        asNutritionist(f.nutritionistAccount, f.patient + "/meal-executions/" + first.id())
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(first.id().toString()));
    }

    @Test
    void linkedNutritionistReadsCategoriesSummaryAndCalendar() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");
        f.link();
        var lunch = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice")));
        executions.create(f.actor(), "k1", write(lunch, LocalTime.NOON, input(lunch.items().getFirst())));

        asNutritionist(f.nutritionistAccount, f.patient + "/daily-meal-categories", "date", DAY.toString())
                .andExpect(status().isOk()).andExpect(jsonPath("$.categories.length()").value(4))
                .andExpect(jsonPath("$.categories[1].state").value("LOGGED"));
        asNutritionist(f.nutritionistAccount, f.patient + "/daily-nutrition-summary", "date", DAY.toString())
                .andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(f.plan.toString()))
                .andExpect(jsonPath("$.realized.energy_kcal.amount").exists());
        asNutritionist(f.nutritionistAccount, f.patient + "/calendar-periods")
                .andExpect(status().isOk()).andExpect(jsonPath("$.plans[0].planId").value(f.plan.toString()));
    }

    @Test
    void nutritionistWithoutAnActiveRelationshipIsRejectedOnEveryRoute() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");
        var stranger = new TrackingFixture(jdbc, "FREE");
        var base = f.patient + "/";

        for (var path : new String[] { "meal-executions", "daily-meal-categories", "daily-nutrition-summary" })
            asNutritionist(
                    stranger.nutritionistAccount, base + path, "date", DAY.toString(), "from", DAY.toString(),
                    "to", DAY.toString()
            ).andExpect(status().isForbidden());
        asNutritionist(stranger.nutritionistAccount, base + "calendar-periods").andExpect(status().isForbidden());
        asNutritionist(stranger.nutritionistAccount, base + "meal-executions/" + UUID.randomUUID())
                .andExpect(status().isForbidden());
    }

    @Test
    void executionOfAnotherPatientIsNotFoundThroughALinkedPatient() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");
        f.link();
        var other = new TrackingFixture(jdbc, "FREE");
        var food = other.food("Rice");
        other.prescribedMeal("LUNCH", food);
        other.publish();
        var foreign = executions.create(
                other.actor(), "k1",
                new com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Write(
                        DAY, LocalTime.NOON,
                        com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType.LUNCH,
                        null, java.util.List.of(free(food))
                )
        );

        asNutritionist(f.nutritionistAccount, f.patient + "/meal-executions/" + foreign.id())
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnInvalidPeriodAndNeverAcceptsWrites() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");
        f.link();

        asNutritionist(
                f.nutritionistAccount, f.patient + "/meal-executions", "from", DAY.toString(), "to",
                DAY.minusDays(1).toString()
        ).andExpect(status().isBadRequest());
        asNutritionist(f.nutritionistAccount, f.patient + "/meal-executions", "from", DAY.toString())
                .andExpect(status().isBadRequest());
        asNutritionist(
                f.nutritionistAccount, f.patient + "/meal-executions", "from", DAY.toString(), "to",
                DAY.plusDays(400).toString()
        ).andExpect(status().isBadRequest());
        asNutritionist(HttpMethod.POST, f.nutritionistAccount, f.patient + "/meal-executions")
                .andExpect(status().is4xxClientError());
        asNutritionist(HttpMethod.DELETE, f.nutritionistAccount, f.patient + "/meal-executions/" + UUID.randomUUID())
                .andExpect(status().is4xxClientError());
    }

    @Test
    void patientRoleCannotUseTheNutritionistRoutes() throws Exception {
        var f = new TrackingFixture(jdbc, "FREE");
        f.link();

        mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/nutritionists/me/patients/" + f.patient + "/calendar-periods")
                        .with(
                                jwt().jwt(t -> t.subject(f.patientAccount.toString()).claim("role", "PATIENT"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                        )
        ).andExpect(status().isForbidden());
    }
}

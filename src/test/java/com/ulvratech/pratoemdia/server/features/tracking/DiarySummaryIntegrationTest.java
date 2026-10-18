package com.ulvratech.pratoemdia.server.features.tracking;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes;

import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.DiaryOperations;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class DiarySummaryIntegrationTest {
    @TestConfiguration
    static class ClockConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>(Instant.now());

        void advanceSeconds(long seconds) {
            current.updateAndGet(value -> value.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DiaryOperations diary;
    @Autowired
    MockMvc mvc;
    @Autowired
    MutableClock clock;

    @Test
    void distinguishesTargetPlannedAndRealizedWithMissingNutrientsAsZero() {
        var account = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var nutritionistAccount = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var prescription = UUID.randomUUID();
        var version = UUID.randomUUID();
        var plan = UUID.randomUUID();
        var meal = UUID.randomUUID();
        var plannedItem = UUID.randomUUID();
        var food = UUID.randomUUID();
        var portion = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT'::user_role)", account,
                account + "@example.com", "hash"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)", patient, account, "Patient"
        );
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'NUTRITIONIST'::user_role)",
                nutritionistAccount, nutritionistAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)", nutritionist,
                nutritionistAccount, "Nutritionist"
        );
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)", prescription, patient,
                nutritionist
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,1,'DRAFT',?)",
                version, prescription, nutritionistAccount
        );
        jdbc.update(
                "insert into prescription_nutrition_target(id,prescription_version_id,nutrient_code,amount,unit) values (?,?,?,?,'g')",
                UUID.randomUUID(), version, "protein", new BigDecimal("75")
        );
        jdbc.update("update prescription_version set status='PUBLISHED',published_at=now() where id=?", version);
        jdbc.update(
                "insert into food(id,name,default_unit,origin,source,source_external_id,source_description) values (?,?,?,'IMPORTED'::food_origin,?,?,?)",
                food, "Test food", "g", "TEST", food.toString(), "Test food"
        );
        jdbc.update(
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,100,'g')", portion, food,
                "100 g"
        );
        jdbc.update(
                "insert into food_portion_nutrient(id,food_portion_id,nutrient_code,amount,unit,value_status) values (?,?,?,?,?,'VALUE')",
                UUID.randomUUID(), portion, "protein", new BigDecimal("10"), "g"
        );
        jdbc.update(
                "insert into weekly_plan(id,patient_id,prescription_version_id,planning_mode,period_start,period_end,confirmation_deadline) values (?,?,?,'FREE','2026-10-12','2026-10-18',now())",
                plan, patient, version
        );
        jdbc.update(
                "insert into planned_meal(id,weekly_plan_id,meal_date,meal_type,name,meal_order) values (?,?,?,'LUNCH','Lunch',0)",
                meal, plan, LocalDate.parse("2026-10-13")
        );
        jdbc.update(
                "insert into planned_meal_item(id,planned_meal_id,food_id,food_portion_id,quantity,unit,item_order) values (?,?,?,?,250,'g',0)",
                plannedItem, meal, food, portion
        );

        var summary = diary.dailySummary(new DiaryTypes.Actor(account, "PATIENT"), LocalDate.parse("2026-10-13"));

        assertEquals(0, new BigDecimal("75").compareTo(summary.prescribed().get("protein").amount()));
        assertEquals(0, new BigDecimal("25").compareTo(summary.planned().get("protein").amount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.realized().get("protein").amount()));
        var otherVersion = UUID.randomUUID();
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,2,'DRAFT',?)",
                otherVersion, prescription, nutritionistAccount
        );
        assertThrows(
                org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                        "insert into prescription_nutrition_target(id,prescription_version_id,nutrient_code,amount,unit) values (?,?,?,?,'mg')",
                        UUID.randomUUID(), otherVersion, "protein", new BigDecimal("75")
                )
        );
        jdbc.update("update food_portion_nutrient set amount=20 where food_portion_id=?", portion);
        var frozen = diary.dailySummary(new DiaryTypes.Actor(account, "PATIENT"), LocalDate.parse("2026-10-13"));
        assertEquals(0, new BigDecimal("25").compareTo(frozen.planned().get("protein").amount()));
        jdbc.update("update food_portion_nutrient set amount=10 where food_portion_id=?", portion);

        try {
            mvc.perform(
                    get("/api/v1/planned-meals/" + meal + "/food-log")
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].catalogFoodId").value(food.toString()));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

        try {
            mvc.perform(
                    get("/api/v1/patients/me/daily-nutrition-summary")
                            .param("date", "2026-10-13")
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.planned.protein.amount").value(25));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

        jdbc.update("update prescription set planning_mode='FREE' where id=?", prescription);
        try {
            var created = mvc.perform(
                    post("/api/v1/patients/me/meal-executions")
                            .header("Idempotency-Key", "free-execution-1")
                            .contentType("application/json")
                            .content(
                                    "{\"date\":\"2026-10-20\",\"time\":\"13:30\",\"mealType\":\"LUNCH\",\"items\":[{\"foodId\":\""
                                            + food
                                            + "\",\"portionId\":\"" + portion
                                            + "\",\"unit\":\"g\",\"quantity\":100,\"multiplier\":1}]}"
                            )
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mode").value("FREE"))
                    .andExpect(jsonPath("$.time").value("13:30:00"))
                    .andExpect(jsonPath("$.mealType").value("LUNCH"))
                    .andReturn();
            var id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id").toString();
            mvc.perform(
                    get("/api/v1/patients/me/meal-executions/" + id)
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totals.protein.amount").value(10));
            mvc.perform(
                    get("/api/v1/patients/me/daily-nutrition-summary")
                            .param("date", "2026-10-20")
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.planId").doesNotExist())
                    .andExpect(jsonPath("$.realized.protein.amount").value(10));
            mvc.perform(
                    put("/api/v1/patients/me/meal-executions/" + id)
                            .header("Idempotency-Key", "replace-execution-1")
                            .header("If-Match", "1")
                            .contentType("application/json")
                            .content(
                                    "{\"date\":\"2026-10-20\",\"time\":\"13:30\",\"mealType\":\"LUNCH\",\"items\":[{\"foodId\":\""
                                            + food
                                            + "\",\"portionId\":\"" + portion
                                            + "\",\"unit\":\"g\",\"quantity\":200,\"multiplier\":1}]}"
                            )
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value(2))
                    .andExpect(jsonPath("$.totals.protein.amount").value(20));
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions")
                            .header("Idempotency-Key", "free-execution-1")
                            .contentType("application/json")
                            .content(
                                    "{\"date\":\"2026-10-20\",\"time\":\"13:30\",\"mealType\":\"LUNCH\",\"items\":[{\"foodId\":\""
                                            + food
                                            + "\",\"portionId\":\"" + portion
                                            + "\",\"unit\":\"g\",\"quantity\":100,\"multiplier\":1}]}"
                            )
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.version").value(1))
                    .andExpect(jsonPath("$.totals.protein.amount").value(10));
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions/" + id + "/move")
                            .header("Idempotency-Key", "move-execution-1")
                            .header("If-Match", "2")
                            .contentType("application/json")
                            .content("{\"date\":\"2026-10-21\"}")
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value(3))
                    .andExpect(jsonPath("$.date").value("2026-10-21"));
            var deleted = mvc.perform(
                    delete("/api/v1/patients/me/meal-executions/" + id)
                            .header("If-Match", "3")
                            .header("Idempotency-Key", "delete-execution-1")
                            .with(
                                    jwt().jwt(token -> token.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restoreToken").exists())
                    .andReturn();
            var token = com.jayway.jsonpath.JsonPath.read(
                    deleted.getResponse().getContentAsString(),
                    "$.restoreToken"
            ).toString();
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions/" + id + "/restore")
                            .contentType("application/json")
                            .content("{\"restoreToken\":\"" + token + "\"}")
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value(5));
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions/" + id + "/restore")
                            .contentType("application/json")
                            .content("{\"restoreToken\":\"" + token + "\"}")
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value(5));
            mvc.perform(
                    get("/api/v1/patients/me/meal-executions")
                            .param("date", "2026-10-21")
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(id));
            jdbc.update("update weekly_plan set planning_mode='RESTRICTED' where id=?", plan);
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions/" + id + "/move")
                            .header("Idempotency-Key", "move-execution-2")
                            .header("If-Match", "5")
                            .contentType("application/json")
                            .content(
                                    "{\"date\":\"2026-10-13\",\"plannedMealId\":\"" + meal
                                            + "\",\"items\":[{\"foodId\":\"" + food + "\",\"portionId\":\""
                                            + portion + "\",\"unit\":\"g\",\"quantity\":200,\"multiplier\":1,"
                                            + "\"plannedItemId\":\"" + plannedItem + "\"}]}"
                            )
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").value("RESTRICTED"))
                    .andExpect(jsonPath("$.plannedMealId").value(meal.toString()));
            mvc.perform(
                    put("/api/v1/patients/me/meal-executions/" + id)
                            .header("Idempotency-Key", "stale-update")
                            .header("If-Match", "5")
                            .contentType("application/json")
                            .content(
                                    "{\"date\":\"2026-10-13\",\"plannedMealId\":\"" + meal
                                            + "\",\"items\":[]}"
                            )
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isConflict());
            jdbc.update("update prescription set planning_mode='RESTRICTED' where id=?", prescription);
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions")
                            .header("Idempotency-Key", "free-with-restricted-prescription")
                            .contentType("application/json")
                            .content("{\"date\":\"2026-10-22\",\"items\":[]}")
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isBadRequest());
            var expiredDelete = mvc.perform(
                    delete("/api/v1/patients/me/meal-executions/" + id)
                            .header("If-Match", "6")
                            .header("Idempotency-Key", "delete-expiring")
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isOk()).andReturn();
            var expiredToken = com.jayway.jsonpath.JsonPath.read(
                    expiredDelete.getResponse().getContentAsString(), "$.restoreToken"
            ).toString();
            clock.advanceSeconds(6);
            mvc.perform(
                    post("/api/v1/patients/me/meal-executions/" + id + "/restore")
                            .contentType("application/json")
                            .content("{\"restoreToken\":\"" + expiredToken + "\"}")
                            .with(
                                    jwt().jwt(jwt -> jwt.subject(account.toString()).claim("role", "PATIENT"))
                                            .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
            )
                    .andExpect(status().isConflict());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}

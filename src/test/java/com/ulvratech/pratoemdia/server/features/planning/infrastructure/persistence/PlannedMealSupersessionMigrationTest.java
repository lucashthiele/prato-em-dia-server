package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** V032 sobre dados legados: duplicatas do LUC-29 e planos excluídos viram histórico. */
@Testcontainers(disabledWithoutDocker = true)
class PlannedMealSupersessionMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void backfillKeepsTheNewestPlanMealAndSupersedesDuplicatesAndDeletedPlans() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        );
        flyway(dataSource, "31").migrate();
        var jdbc = new JdbcTemplate(dataSource);
        var patient = legacyPatient(jdbc);
        var version = legacyVersion(jdbc, patient);
        var day = LocalDate.of(2026, 10, 14);
        var older = legacyPlan(jdbc, patient, version, "2026-10-12", "2026-01-01T00:00:00Z", true);
        var newer = legacyPlan(jdbc, patient, version, "2026-10-14", "2026-02-01T00:00:00Z", true);
        var deleted = legacyPlan(jdbc, patient, version, "2026-10-14", "2026-03-01T00:00:00Z", false);
        var olderMeal = legacyMeal(jdbc, older, day, "LUNCH", 0);
        var newerMeal = legacyMeal(jdbc, newer, day, "LUNCH", 0);
        var deletedMeal = legacyMeal(jdbc, deleted, day.plusDays(1), "LUNCH", 0);
        var untouched = legacyMeal(jdbc, older, day.plusDays(2), "LUNCH", 0);

        flyway(dataSource, "latest").migrate();

        assertNotNull(supersededAt(jdbc, olderMeal));
        assertNull(supersededAt(jdbc, newerMeal));
        assertNotNull(supersededAt(jdbc, deletedMeal));
        assertNull(supersededAt(jdbc, untouched));
        assertEquals(
                patient, jdbc.queryForObject("select patient_id from planned_meal where id = ?", UUID.class, newerMeal)
        );
    }

    private static Flyway flyway(DriverManagerDataSource dataSource, String target) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(target).load();
    }

    private static Object supersededAt(JdbcTemplate jdbc, UUID meal) {
        return jdbc.queryForObject("select superseded_at from planned_meal where id = ?", Object.class, meal);
    }

    private static UUID legacyPatient(JdbcTemplate jdbc) {
        var account = UUID.randomUUID();
        var patient = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT'::user_role)", account,
                account + "@example.com", "hash"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)", patient, account, "Patient"
        );
        return patient;
    }

    private static UUID legacyVersion(JdbcTemplate jdbc, UUID patient) {
        var account = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var prescription = UUID.randomUUID();
        var version = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'NUTRITIONIST'::user_role)",
                account, account + "@example.com", "hash"
        );
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)", nutritionist, account,
                "Nutritionist"
        );
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)", prescription, patient,
                nutritionist
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,1,'DRAFT',?)",
                version, prescription, account
        );
        return version;
    }

    private static UUID legacyPlan(
            JdbcTemplate jdbc, UUID patient, UUID version, String start, String createdAt, boolean active
    ) {
        var plan = UUID.randomUUID();
        jdbc.update(
                "insert into weekly_plan(id,patient_id,prescription_version_id,planning_mode,period_start,period_end,confirmation_deadline,created_at,active) values (?,?,?,'FREE',?::date,?::date + 6,now(),?::timestamptz,?)",
                plan, patient, version, start, start, createdAt, active
        );
        return plan;
    }

    private static UUID legacyMeal(JdbcTemplate jdbc, UUID plan, LocalDate date, String type, int order) {
        var meal = UUID.randomUUID();
        jdbc.update(
                "insert into planned_meal(id,weekly_plan_id,meal_date,meal_type,name,meal_order) values (?,?,?,?::meal_type,'Meal',?)",
                meal, plan, date, type, order
        );
        return meal;
    }
}

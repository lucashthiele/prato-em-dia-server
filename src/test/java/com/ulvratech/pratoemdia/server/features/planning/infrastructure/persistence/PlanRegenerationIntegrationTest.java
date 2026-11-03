package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PreferenceUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningCommands;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningQueries;
import com.ulvratech.pratoemdia.server.features.planning.application.usecase.WeeklyPlanningUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
class PlanRegenerationIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlanningCommands commands;
    @Autowired
    PlanningQueries queries;

    UUID account;
    UUID patient;
    Actor actor;
    LocalDate today = LocalDate.now(WeeklyPlanningUseCase.ZONE);

    @BeforeEach
    void patientWithPublishedPrescription() {
        account = UUID.randomUUID();
        patient = UUID.randomUUID();
        actor = new Actor(account, "PATIENT");
        var nutritionistAccount = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var prescription = UUID.randomUUID();
        var version = UUID.randomUUID();
        var food = UUID.randomUUID();
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT'::user_role)", account,
                account + "@example.com", "hash"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)", patient, account,
                "Patient"
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
                "insert into prescription(id,patient_id,nutritionist_id,planning_mode) values (?,?,?,'FREE')",
                prescription, patient, nutritionist
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,1,'DRAFT',?)",
                version, prescription, nutritionistAccount
        );
        jdbc.update(
                "insert into food(id,name,default_unit,created_by_user_id,origin) values (?,?,?,?,'NUTRITIONIST'::food_origin)",
                food, "Food " + food, "g", nutritionistAccount
        );
        meal(version, food, "Breakfast", 0, "BREAKFAST");
        meal(version, food, "Lunch", 1, "LUNCH");
        jdbc.update("update prescription_version set status = 'PUBLISHED', published_at = now() where id = ?", version);
    }

    private void meal(UUID version, UUID food, String name, int order, String type) {
        var meal = UUID.randomUUID();
        jdbc.update(
                "insert into prescribed_meal(id,prescription_version_id,name,meal_order,meal_type) values (?,?,?,?,?::meal_type)",
                meal, version, name, order, type
        );
        jdbc.update(
                "insert into prescribed_meal_item(id,prescribed_meal_id,food_id,quantity,unit,item_order) values (?,?,?,1,'g',0)",
                UUID.randomUUID(), meal, food
        );
    }

    private void automaticDayIs(LocalDate day) {
        commands.updatePreference(new PreferenceUpdate(actor, day.getDayOfWeek(), true));
    }

    private int activeMeals() {
        return jdbc.queryForObject(
                "select count(*) from planned_meal where patient_id = ? and superseded_at is null", Integer.class,
                patient
        );
    }

    private int distinctActiveSlots() {
        return jdbc.queryForObject(
                "select count(*) from (select distinct meal_date, meal_type from planned_meal where patient_id = ? and superseded_at is null) s",
                Integer.class, patient
        );
    }

    @Test
    void firstManualGenerationCoversTodayUpToTheNextAutomaticDay() {
        automaticDayIs(today.plusDays(3));

        var plan = commands.generate(actor);

        assertEquals(today, plan.periodStart());
        assertEquals(today.plusDays(3), plan.periodEnd());
        assertEquals(8, plan.meals().size());
        assertEquals(8, activeMeals());
    }

    @Test
    void regeneratingTheSameWindowReplacesTheMealsInsteadOfDuplicatingThem() {
        automaticDayIs(today.plusDays(3));
        var first = commands.generate(actor);

        var second = commands.generate(actor);

        assertEquals(8, activeMeals());
        assertEquals(8, distinctActiveSlots());
        assertEquals(8, second.meals().size());
        assertEquals(List.of(second.id()), queries.list(actor).stream().map(WeeklyPlanOutput::id).toList());
        var history = queries.get(new PlanQuery(actor, first.id()));
        assertEquals(0, history.meals().size());
    }

    @Test
    void partialRegenerationReplacesOnlyTheCoveredDaysAndKeepsTheRestInTheOldPlan() {
        automaticDayIs(today.plusDays(3));
        var first = commands.generate(actor);
        automaticDayIs(today.plusDays(1));

        var partial = commands.generate(actor);

        assertEquals(today, partial.periodStart());
        assertEquals(today.plusDays(1), partial.periodEnd());
        assertEquals(4, partial.meals().size());
        var old = queries.get(new PlanQuery(actor, first.id()));
        assertEquals(4, old.meals().size());
        assertTrue(old.meals().stream().allMatch(meal -> meal.date().isAfter(today.plusDays(1))));
        assertEquals(8, activeMeals());
        assertEquals(8, distinctActiveSlots());
    }

    @Test
    void databaseRejectsASecondActiveMealForTheSameDayAndCategory() {
        automaticDayIs(today.plusDays(3));
        var plan = commands.generate(actor);
        var meal = plan.meals().getFirst();
        var other = commands.generate(actor);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "insert into planned_meal(id,weekly_plan_id,patient_id,meal_date,meal_type,name,meal_order) values (?,?,?,?,?::meal_type,'Dup',9)",
                        UUID.randomUUID(), other.id(), patient, meal.date(), meal.mealType().name()
                )
        );
    }

    @Test
    void databaseRejectsAPlannedMealWhosePatientDiffersFromThePlanOwner() {
        automaticDayIs(today.plusDays(3));
        var plan = commands.generate(actor);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "insert into planned_meal(id,weekly_plan_id,patient_id,meal_date,meal_type,name,meal_order) values (?,?,?,?,'SNACK'::meal_type,'Wrong',9)",
                        UUID.randomUUID(), plan.id(), UUID.randomUUID(), today.minusDays(30)
                )
        );
    }

    @Test
    void deletingAPlanFreesItsDaysForANewOne() {
        automaticDayIs(today.plusDays(3));
        var plan = commands.generate(actor);

        commands.delete(new PlanQuery(actor, plan.id()));

        assertEquals(0, activeMeals());
        assertEquals(8, commands.generate(actor).meals().size());
    }
}

package com.ulvratech.pratoemdia.server.features.shopping;

import com.ulvratech.pratoemdia.server.support.TrackingFixture;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class ShoppingListIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static final LocalDate DAY = LocalDate.parse("2026-10-13");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MockMvc mvc;

    ResultActions call(HttpMethod method, TrackingFixture f, UUID plan) throws Exception {
        return call(method, f.patientAccount, "PATIENT", plan);
    }

    ResultActions call(HttpMethod method, UUID account, String role, UUID plan) throws Exception {
        return mvc.perform(
                MockMvcRequestBuilders.request(method, "/api/v1/patients/me/weekly-plans/" + plan + "/shopping-list")
                        .with(
                                jwt().jwt(t -> t.subject(account.toString()).claim("role", role))
                                        .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                        )
        );
    }

    /** Two days with the same lunch (rice 100 g, beans 100 g) and a snack (apple). */
    TrackingFixture plannedWeek() {
        return plannedWeek("RESTRICTED");
    }

    TrackingFixture plannedWeek(String mode) {
        var f = new TrackingFixture(jdbc, mode);
        var lunch = f.prescribedMeal("LUNCH", f.food("Rice"), f.food("Beans"));
        var snack = f.prescribedMeal("SNACK", f.food("Apple"));
        f.plannedMeal(DAY, lunch);
        f.plannedMeal(DAY.plusDays(1), lunch);
        f.plannedMeal(DAY, snack);
        return f;
    }

    @Test
    void createsTheListFromThePlannedMealsAndIsIdempotent() throws Exception {
        var f = plannedWeek();

        call(HttpMethod.GET, f, f.plan).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHOPPING_LIST_NOT_FOUND"));
        var body = call(HttpMethod.POST, f, f.plan).andExpect(status().isCreated())
                .andExpect(jsonPath("$.weeklyPlanId").value(f.plan.toString()))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].foodName").value("Rice"))
                .andExpect(jsonPath("$.items[0].portionLabel").value("Rice 100 g"))
                .andExpect(jsonPath("$.items[0].unit").value("g"))
                .andExpect(jsonPath("$.items[0].quantity").value(200.0))
                .andExpect(jsonPath("$.items[2].foodName").value("Apple"))
                .andExpect(jsonPath("$.items[2].quantity").value(100.0))
                .andReturn().getResponse().getContentAsString();
        var id = com.jayway.jsonpath.JsonPath.<String>read(body, "$.id");

        call(HttpMethod.POST, f, f.plan).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        call(HttpMethod.GET, f, f.plan).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertEquals(
                1,
                jdbc.queryForObject("select count(*) from shopping_list where weekly_plan_id=?", Integer.class, f.plan)
        );
    }

    @Test
    void changingAPlannedItemDeactivatesTheListAndAllowsANewOne() throws Exception {
        var f = plannedWeek();
        var first = call(HttpMethod.POST, f, f.plan).andReturn().getResponse().getContentAsString();
        var firstId = com.jayway.jsonpath.JsonPath.<String>read(first, "$.id");

        jdbc.update(
                "update planned_meal_item set quantity=300 where id=(select i.id from planned_meal_item i join planned_meal m on m.id=i.planned_meal_id join food fo on fo.id=i.food_id where m.weekly_plan_id=? and fo.name='Rice' limit 1)",
                f.plan
        );

        call(HttpMethod.GET, f, f.plan).andExpect(status().isNotFound());
        call(HttpMethod.POST, f, f.plan).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(firstId)))
                .andExpect(jsonPath("$.items[0].quantity").value(400.0));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from shopping_list where weekly_plan_id=? and not active", Integer.class,
                        f.plan
                )
        );
    }

    @Test
    void anyChangeToThePlannedMealsDeactivatesTheList() throws Exception {
        var changes = new String[] {
                "update planned_meal set name='Almoço' where weekly_plan_id=? and meal_type='LUNCH' and meal_date=date '2026-10-13'",
                "delete from planned_meal_item where planned_meal_id in (select id from planned_meal where weekly_plan_id=? and meal_type='SNACK')",
                "delete from planned_meal where weekly_plan_id=? and meal_type='LUNCH' and meal_date=date '2026-10-14'",
                "update weekly_plan set active=false where id=?" };
        for (var change : changes) {
            var f = plannedWeek();
            call(HttpMethod.POST, f, f.plan).andExpect(status().isCreated());

            jdbc.update(change, f.plan);

            assertEquals(
                    0,
                    jdbc.queryForObject(
                            "select count(*) from shopping_list where weekly_plan_id=? and active", Integer.class,
                            f.plan
                    ),
                    change
            );
        }
    }

    @Test
    void confirmingThePlanKeepsTheList() throws Exception {
        var f = plannedWeek();
        call(HttpMethod.POST, f, f.plan).andExpect(status().isCreated());

        jdbc.update("update weekly_plan set status='CONFIRMED', confirmed_at=now() where id=?", f.plan);

        call(HttpMethod.GET, f, f.plan).andExpect(status().isOk());
    }

    @Test
    void theDatabaseAllowsOnlyOneActiveListPerPlan() {
        var f = plannedWeek();
        jdbc.update(
                "insert into shopping_list(id,weekly_plan_id,patient_id) values (?,?,?)", UUID.randomUUID(), f.plan,
                f.patient
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "insert into shopping_list(id,weekly_plan_id,patient_id) values (?,?,?)", UUID.randomUUID(),
                        f.plan, f.patient
                )
        );
    }

    @Test
    void onlyThePlanOwnerCanUseTheList() throws Exception {
        var f = plannedWeek();
        var other = new TrackingFixture(jdbc, "RESTRICTED");

        call(HttpMethod.POST, other, f.plan).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHOPPING_PLAN_NOT_FOUND"));
        call(HttpMethod.GET, other, f.plan).andExpect(status().isNotFound());
        call(HttpMethod.POST, f, UUID.randomUUID()).andExpect(status().isNotFound());
        call(HttpMethod.POST, f.nutritionistAccount, "NUTRITIONIST", f.plan).andExpect(status().isForbidden());
    }

    @Test
    void deletedPlanHasNoShoppingList() throws Exception {
        var f = plannedWeek();
        jdbc.update("update weekly_plan set active=false where id=?", f.plan);

        call(HttpMethod.POST, f, f.plan).andExpect(status().isNotFound());
    }

    @Autowired
    com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningCommands planningCommands;
    @Autowired
    com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningQueries planningQueries;

    @Test
    void planningUseCasesDeactivateTheListThroughTheirOwnWrites() throws Exception {
        var f = plannedWeek("FREE");
        var actor = new com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor(
                f.patientAccount, "PATIENT"
        );
        var query = new com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery(
                actor, f.plan
        );
        jdbc.update(
                "update planned_meal set meal_date = meal_date + (current_date + 1 - date '2026-10-12') where weekly_plan_id=?",
                f.plan
        );
        jdbc.update(
                "update weekly_plan set period_start = current_date + 1, period_end = current_date + 7 where id=?",
                f.plan
        );
        call(HttpMethod.POST, f, f.plan).andExpect(status().isCreated());

        var plan = planningQueries.get(query);
        var meals = plan.meals().stream().map(
                meal -> new com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.MealInput(
                        meal.id(), meal.date(), meal.mealType(), meal.name(), meal.order(),
                        meal.items().stream().map(
                                item -> new com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.ItemInput(
                                        item.id(), item.foodId(), item.foodPortionId(),
                                        item.foodId().equals(plan.meals().getFirst().items().getFirst().foodId())
                                                ? new java.math.BigDecimal("250")
                                                : item.quantity(),
                                        item.unit(), item.order()
                                )
                        ).toList()
                )
        ).toList();

        planningCommands.update(
                new com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanUpdate(
                        actor, f.plan, plan.version(), meals
                )
        );
        call(HttpMethod.GET, f, f.plan).andExpect(status().isNotFound());

        call(HttpMethod.POST, f, f.plan).andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(500.0));
        planningCommands.confirm(query);
        call(HttpMethod.GET, f, f.plan).andExpect(status().isOk());

        planningCommands.delete(query);
        call(HttpMethod.GET, f, f.plan).andExpect(status().isNotFound());
        assertEquals(
                0,
                jdbc.queryForObject(
                        "select count(*) from shopping_list where weekly_plan_id=? and active", Integer.class, f.plan
                )
        );
    }
}

package com.ulvratech.pratoemdia.server.features.tracking;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.ulvratech.pratoemdia.server.features.tracking.TrackingFixture.input;
import static com.ulvratech.pratoemdia.server.features.tracking.TrackingFixture.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MultipleExecutionsIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static final LocalDate DAY = LocalDate.parse("2026-10-13");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ExecutionOperations executions;

    @Test
    void acceptsSeveralExecutionsInTheSamePlannedMeal() {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        var meal = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice"), f.food("Beans")));

        var first = executions.create(
                f.actor(), "k1", write(meal, LocalTime.of(12, 0), input(meal.items().get(0)))
        );
        var second = executions.create(
                f.actor(), "k2", write(meal, LocalTime.of(13, 30), input(meal.items().get(1)))
        );

        assertEquals(meal.id(), first.plannedMealId());
        assertEquals(meal.id(), second.plannedMealId());
        assertEquals(2, executions.list(f.actor(), DAY).size());
    }

    @Test
    void acceptsAnExecutionThatCoversOnlySomePlannedItems() {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        var meal = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice"), f.food("Beans")));

        var created = executions.create(
                f.actor(), "partial", write(meal, LocalTime.NOON, input(meal.items().get(0)))
        );

        assertEquals(1, created.items().size());
    }

    @Test
    void stillRejectsRepeatedPlannedItemInTheSameExecution() {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        var meal = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice")));
        var item = input(meal.items().get(0));

        var error = assertThrows(
                TrackingException.class,
                () -> executions.create(f.actor(), "dup", write(meal, LocalTime.NOON, item, item))
        );
        assertEquals(TrackingException.Kind.INVALID, error.kind());
    }

    @Test
    void movesAnExecutionToAMealThatAlreadyHasExecutions() {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        var prescribed = f.prescribedMeal("LUNCH", f.food("Rice"));
        var today = f.plannedMeal(DAY, prescribed);
        var tomorrow = f.plannedMeal(DAY.plusDays(1), prescribed);
        executions.create(
                f.actor(), "t", write(tomorrow, LocalTime.NOON, input(tomorrow.items().get(0)))
        );
        var moving = executions.create(
                f.actor(), "m", write(today, LocalTime.of(12, 30), input(today.items().get(0)))
        );

        var moved = executions.move(
                f.actor(), moving.id(), moving.version(), "move",
                new ExecutionTypes.Write(
                        tomorrow.date(), null, null, tomorrow.id(),
                        java.util.List.of(input(tomorrow.items().get(0)))
                )
        );

        assertEquals(tomorrow.id(), moved.plannedMealId());
        assertEquals(2, executions.list(f.actor(), tomorrow.date()).size());
    }
}

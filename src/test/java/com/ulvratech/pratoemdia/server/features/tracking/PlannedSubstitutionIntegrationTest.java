package com.ulvratech.pratoemdia.server.features.tracking;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import com.ulvratech.pratoemdia.server.support.TrackingFixture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.ulvratech.pratoemdia.server.support.TrackingFixture.input;
import static com.ulvratech.pratoemdia.server.support.TrackingFixture.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * P-05: a swap made in the weekly plan (the planned item already is the substitute food) keeps the
 * "Com substituição" mark when the patient logs that item.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PlannedSubstitutionIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static final LocalDate DAY = LocalDate.parse("2026-10-13");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ExecutionOperations executions;

    record Swapped(TrackingFixture fixture, TrackingFixture.PlannedMeal meal, TrackingFixture.Food quinoa,
            UUID substitutionId) {
    }

    /** Lunch with rice, quinoa authorized for it, and the plan already swapped to quinoa. */
    Swapped swappedLunch() {
        var f = new TrackingFixture(jdbc, "SUBSTITUTIONS_ONLY");
        var quinoa = f.food("Quinoa");
        var prescribed = f.prescribedMeal("LUNCH", f.food("Rice"));
        var prescribedItem = prescribed.items().getFirst();
        f.substitution(prescribedItem, quinoa);
        var substitutionId = f.substitutionId(prescribedItem);
        var meal = f.plannedMeal(DAY, prescribed);
        jdbc.update(
                "update planned_meal_item set food_id=?, food_portion_id=?, allowed_substitution_id=? where id=?",
                quinoa.foodId(), quinoa.portionId(), substitutionId, meal.items().getFirst().id()
        );
        return new Swapped(f, meal, quinoa, substitutionId);
    }

    ExecutionTypes.ItemInput quinoaFor(Swapped s, UUID allowedSubstitutionId) {
        return new ExecutionTypes.ItemInput(
                s.quinoa().foodId(), s.quinoa().portionId(), "g", new BigDecimal("100"), BigDecimal.ONE,
                s.meal().items().getFirst().id(), allowedSubstitutionId
        );
    }

    @Test
    void loggingASwappedPlannedItemInheritsTheSubstitution() {
        var s = swappedLunch();

        var created = executions.create(
                s.fixture().actor(), "inherit", write(s.meal(), LocalTime.NOON, quinoaFor(s, null))
        );

        assertEquals(s.substitutionId(), created.items().getFirst().allowedSubstitutionId());
    }

    @Test
    void acceptsTheSwapReferenceTheClientAlreadySends() {
        var s = swappedLunch();

        var created = executions.create(
                s.fixture().actor(), "explicit",
                write(s.meal(), LocalTime.NOON, quinoaFor(s, s.substitutionId()))
        );

        assertEquals(s.substitutionId(), created.items().getFirst().allowedSubstitutionId());
    }

    @Test
    void anItemThatWasNotSwappedStaysWithoutTheMark() {
        var f = new TrackingFixture(jdbc, "SUBSTITUTIONS_ONLY");
        var meal = f.plannedMeal(DAY, f.prescribedMeal("LUNCH", f.food("Rice")));

        var created = executions.create(
                f.actor(), "plain", write(meal, LocalTime.NOON, input(meal.items().getFirst()))
        );

        assertNull(created.items().getFirst().allowedSubstitutionId());
    }

    @Test
    void stillRejectsAReferenceThatIsNotThePlanSwap() {
        var s = swappedLunch();

        var error = assertThrows(
                TrackingException.class,
                () -> executions.create(
                        s.fixture().actor(), "other",
                        write(s.meal(), LocalTime.NOON, quinoaFor(s, UUID.randomUUID()))
                )
        );
        assertEquals(TrackingException.Kind.INVALID, error.kind());
    }
}

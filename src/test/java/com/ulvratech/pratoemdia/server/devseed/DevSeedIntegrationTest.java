package com.ulvratech.pratoemdia.server.devseed;

import com.ulvratech.pratoemdia.server.devseed.config.DevSeedProperties;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class DevSeedIntegrationTest {
    private static final List<String> SEEDED_TABLES = List.of(
            "user_account", "nutritionist_profile", "patient_profile", "nutritionist_patient_relationship",
            "prescription", "prescription_version", "prescribed_meal", "prescribed_meal_item",
            "allowed_substitution", "prescription_nutrition_target", "weekly_planning_preference", "weekly_plan",
            "planned_meal", "planned_meal_item", "planned_meal_item_nutrient_snapshot", "meal_execution",
            "meal_execution_item", "meal_execution_nutrient_snapshot", "meal_execution_command"
    );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TransactionTemplate transaction;
    @Autowired
    PasswordHasher passwordHasher;
    @TempDir
    Path directory;

    @BeforeEach
    void catalog() {
        if (count("food") > 0) {
            return;
        }
        for (int index = 0; index < 20; index++) {
            var food = UUID.randomUUID();
            var portion = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO food (id, name, default_unit, source, source_external_id, source_description)
                    VALUES (?, ?, 'g', 'TEST', ?, 'Test catalog')
                    """, food, "Alimento " + index, "food-" + index);
            jdbc.update("""
                    INSERT INTO food_portion (id, food_id, label, quantity, unit, grams_equivalent, source_type)
                    VALUES (?, ?, '100 g', 100, 'g', 100, 'STANDARD')
                    """, portion, food);
            for (var nutrient : List.of(
                    new String[] { "energy_kcal", "kcal" }, new String[] { "protein", "g" },
                    new String[] { "carbohydrate", "g" }, new String[] { "lipids", "g" },
                    new String[] { "dietary_fiber", "g" }
            )) {
                jdbc.update(
                        """
                                INSERT INTO food_portion_nutrient (id, food_portion_id, nutrient_code, amount, unit, value_status)
                                VALUES (?, ?, ?, 10 + ?, ?, 'VALUE')
                                """,
                        UUID.randomUUID(), portion, nutrient[0], index, nutrient[1]
                );
            }
        }
    }

    @Test
    void fillsEverySeededTableWithoutTouchingTheCatalogAndStaysIdempotent() throws Exception {
        var properties = properties(false);
        var foods = count("food");

        run(properties);

        for (var table : SEEDED_TABLES) {
            assertThat(count(table)).as(table).isPositive();
        }
        assertThat(count("food")).isEqualTo(foods);
        var counts = SEEDED_TABLES.stream().map(this::count).toList();

        run(properties);
        assertThat(SEEDED_TABLES.stream().map(this::count).toList()).isEqualTo(counts);

        run(properties(true));
        assertThat(SEEDED_TABLES.stream().map(this::count).toList()).isEqualTo(counts);
    }

    @Test
    void createsAccountsThatCanLogInAndDocumentsThem() throws Exception {
        var properties = properties(true);
        run(properties);

        for (var account : SeedAccount.ALL) {
            var hash = jdbc.queryForObject(
                    "SELECT password_hash FROM user_account WHERE email = ?", String.class, account.email()
            );
            assertThat(passwordHasher.matches(properties.getPassword(), hash)).as(account.email()).isTrue();
        }
        var doc = Files.readString(properties.getCredentialsFile());
        assertThat(doc).contains(properties.getPassword());
        SeedAccount.ALL.forEach(account -> assertThat(doc).contains(account.email()));
    }

    @Test
    void publishedPrescriptionsAreFinalizedAndEachPatientHasAtMostOnePublishedVersion() throws Exception {
        run(properties(true));

        assertThat(jdbc.queryForList("""
                SELECT prescription_id FROM prescription_version WHERE status = 'PUBLISHED'
                GROUP BY prescription_id HAVING count(*) > 1
                """)).isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT DISTINCT status::text FROM prescription_version
                WHERE prescription_id IN (SELECT id FROM prescription WHERE patient_id IN (
                    SELECT id FROM patient_profile WHERE user_account_id IN (
                        SELECT id FROM user_account WHERE email LIKE '%@seed.pratoemdia.local')))
                """, String.class)).containsExactlyInAnyOrder("DRAFT", "PUBLISHED", "ARCHIVED");
    }

    private DevSeedProperties properties(boolean reset) {
        var properties = new DevSeedProperties();
        properties.setReset(reset);
        properties.setCredentialsFile(directory.resolve("credentials.md"));
        return properties;
    }

    private void run(DevSeedProperties properties) throws Exception {
        new DevSeedRunner(properties, jdbc, transaction, passwordHasher).run(new DefaultApplicationArguments());
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}

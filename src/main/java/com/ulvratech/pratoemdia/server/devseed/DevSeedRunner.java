package com.ulvratech.pratoemdia.server.devseed;

import com.ulvratech.pratoemdia.server.devseed.config.DevSeedProperties;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.time.LocalDate;

public class DevSeedRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevSeedRunner.class);

    private final DevSeedProperties properties;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final PasswordHasher passwordHasher;

    public DevSeedRunner(
            DevSeedProperties properties,
            JdbcTemplate jdbc,
            TransactionTemplate transaction,
            PasswordHasher passwordHasher
    ) {
        this.properties = properties;
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        LocalDatabaseGuard.requireLocal(jdbc.execute((Connection connection) -> connection.getMetaData().getURL()));
        transaction.executeWithoutResult(status -> seed());
        new CredentialsDocWriter().write(properties.getCredentialsFile(), properties.getPassword());
        LOGGER.info("Credentials written to {}", properties.getCredentialsFile().toAbsolutePath());
    }

    private void seed() {
        var seedReset = new SeedReset(jdbc);
        if (properties.isReset()) {
            LOGGER.info("Removing previously seeded data");
            seedReset.reset();
        }
        if (alreadySeeded()) {
            LOGGER.warn(
                    "Seed accounts already exist; nothing was inserted. Use --devseed.reset=true to recreate them."
            );
            return;
        }
        var today = LocalDate.now();
        var foods = SeedFoods.load(jdbc);
        new IdentitySeeder(jdbc, passwordHasher).seed(properties.getPassword());
        new PrescriptionSeeder(jdbc, foods).seed();
        new PlanningSeeder(jdbc).seed(today);
        new TrackingSeeder(jdbc, foods).seed(today);
        LOGGER.info("Seeded {} accounts", SeedAccount.ALL.size());
    }

    private boolean alreadySeeded() {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM user_account WHERE email = ?)", Boolean.class,
                        SeedAccount.ADMIN.email()
                )
        );
    }
}

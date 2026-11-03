package com.ulvratech.pratoemdia.server.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentProfilesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void everyProfileValidatesTheSchemaInsteadOfLettingHibernateChangeIt() {
        runner.run(context -> {
            var environment = context.getEnvironment();
            assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("true", environment.getProperty("spring.flyway.validate-on-migrate"));
        });
    }

    @Test
    void prodProfileReadsTheDatabaseFromEnvironmentVariablesAndNeverStartsDockerCompose() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "DATABASE_URL=jdbc:postgresql://postgres:5432/pratoemdia",
                "DATABASE_USERNAME=app",
                "DATABASE_PASSWORD=secret"
        ).run(context -> {
            var environment = context.getEnvironment();
            assertEquals(
                    "jdbc:postgresql://postgres:5432/pratoemdia",
                    environment.getProperty("spring.datasource.url")
            );
            assertEquals("app", environment.getProperty("spring.datasource.username"));
            assertEquals("secret", environment.getProperty("spring.datasource.password"));
            assertEquals("false", environment.getProperty("spring.docker.compose.enabled"));
        });
    }

    @Test
    void prodProfileHidesTheSwaggerUi() {
        runner.withPropertyValues("spring.profiles.active=prod").run(context -> {
            var environment = context.getEnvironment();
            assertEquals("false", environment.getProperty("springdoc.swagger-ui.enabled"));
        });
    }
}

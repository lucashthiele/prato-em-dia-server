package com.ulvratech.pratoemdia.server.devseed.config;

import com.ulvratech.pratoemdia.server.devseed.DevSeedRunner;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("dev-seed")
@EnableConfigurationProperties(DevSeedProperties.class)
public class DevSeedConfiguration {
    @Bean
    ApplicationRunner devSeedRunner(
            DevSeedProperties properties,
            JdbcTemplate jdbc,
            TransactionTemplate transaction,
            PasswordHasher passwordHasher,
            ConfigurableApplicationContext context
    ) {
        var runner = new DevSeedRunner(properties, jdbc, transaction, passwordHasher);
        return arguments -> {
            runner.run(arguments);
            // Scheduler threads of the shared context are non-daemon and would keep the JVM alive.
            SpringApplication.exit(context);
        };
    }
}

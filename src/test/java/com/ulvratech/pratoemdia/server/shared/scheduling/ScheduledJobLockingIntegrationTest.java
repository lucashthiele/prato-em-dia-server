package com.ulvratech.pratoemdia.server.shared.scheduling;

import com.ulvratech.pratoemdia.server.features.planning.application.usecase.WeeklyPlanningUseCase;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.scheduling.WeeklyPlanningScheduler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "app.scheduling.enabled=true"
})
class ScheduledJobLockingIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    WeeklyPlanningScheduler scheduler;

    @Autowired
    LockProvider lockProvider;

    @MockitoSpyBean
    WeeklyPlanningUseCase reconciliation;

    @Test
    void jobDoesNotRunWhileAnotherColorHoldsItsLock() {
        var heldByOtherColor = lockProvider.lock(
                new LockConfiguration(Instant.now(), "weeklyPlanGeneration", Duration.ofMinutes(5), Duration.ZERO)
        );
        assertTrue(heldByOtherColor.isPresent());

        scheduler.generate();

        verify(reconciliation, never()).generateDuePlans();
    }

    @Test
    void jobRunsWhenNoOtherColorHoldsItsLock() {
        scheduler.confirm();

        verify(reconciliation).confirmDuePlans();
    }
}

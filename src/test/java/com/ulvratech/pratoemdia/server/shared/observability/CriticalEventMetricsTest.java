package com.ulvratech.pratoemdia.server.shared.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CriticalEventMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CriticalEventMetrics metrics = new CriticalEventMetrics(registry);

    @Test
    void countersExistAtZeroSoThatAlertRulesCanComputeIncreaseFromTheFirstEvent() {
        assertNotNull(registry.find("prato.email.outbox.dead").counter());
        assertEquals(0.0, registry.get("prato.email.outbox.dead").counter().count());
        assertEquals(0.0, registry.get("prato.plan.generation.failed").counter().count());
    }

    @Test
    void emailMovedToDeadIsCounted() {
        metrics.emailOutboxDead();
        metrics.emailOutboxDead();

        assertEquals(2.0, registry.get("prato.email.outbox.dead").counter().count());
    }

    @Test
    void planGenerationThatFailedAllAttemptsIsCounted() {
        metrics.planGenerationFailed();

        assertEquals(1.0, registry.get("prato.plan.generation.failed").counter().count());
    }
}

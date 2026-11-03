package com.ulvratech.pratoemdia.server.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Eventos críticos do negócio que viram métricas Prometheus e, por regra de alerta, mensagens no
 * Discord (deploy/observability/alert-rules.yaml). No Prometheus os nomes viram
 * {@code prato_email_outbox_dead_total} e {@code prato_plan_generation_failed_total}.
 */
@Component
public class CriticalEventMetrics {
    private final Counter emailOutboxDead;
    private final Counter planGenerationFailed;

    public CriticalEventMetrics(MeterRegistry registry) {
        this.emailOutboxDead = Counter.builder("prato.email.outbox.dead")
                .description("E-mails do outbox que esgotaram as tentativas (status DEAD)")
                .register(registry);
        this.planGenerationFailed = Counter.builder("prato.plan.generation.failed")
                .description("Gerações de plano semanal que falharam nas três tentativas")
                .register(registry);
    }

    public void emailOutboxDead() {
        emailOutboxDead.increment();
    }

    public void planGenerationFailed() {
        planGenerationFailed.increment();
    }
}

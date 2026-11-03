package com.ulvratech.pratoemdia.server.features.planning.infrastructure.scheduling;

import com.ulvratech.pratoemdia.server.features.planning.application.port.in.ReconcileWeeklyPlanning;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class WeeklyPlanningScheduler {
    private final ReconcileWeeklyPlanning reconciliation;

    public WeeklyPlanningScheduler(ReconcileWeeklyPlanning reconciliation) {
        this.reconciliation = reconciliation;
    }

    @SchedulerLock(name = "weeklyPlanGeneration", lockAtLeastFor = "PT30S")
    @Scheduled(cron = "0 0,10,20 8 * * *", zone = "America/Sao_Paulo")
    public void generate() {
        reconciliation.generateDuePlans();
    }

    @SchedulerLock(name = "weeklyPlanConfirmation", lockAtLeastFor = "PT30S")
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void confirm() {
        reconciliation.confirmDuePlans();
    }
}

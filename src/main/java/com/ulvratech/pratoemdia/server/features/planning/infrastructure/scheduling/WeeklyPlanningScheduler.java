package com.ulvratech.pratoemdia.server.features.planning.infrastructure.scheduling;

import com.ulvratech.pratoemdia.server.features.planning.application.port.in.ReconcileWeeklyPlanning;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
public class WeeklyPlanningScheduler {
  private final ReconcileWeeklyPlanning reconciliation;

  public WeeklyPlanningScheduler(ReconcileWeeklyPlanning reconciliation) {
    this.reconciliation = reconciliation;
  }

  @Scheduled(cron = "0 */15 8-23 * * *", zone = "America/Sao_Paulo")
  public void generate() {
    reconciliation.generateDuePlans();
  }

  @Scheduled(cron = "0 */5 * * * *", zone = "America/Sao_Paulo")
  public void confirm() {
    reconciliation.confirmDuePlans();
  }
}

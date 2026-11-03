package com.ulvratech.pratoemdia.server.features.planning.infrastructure.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class WeeklyPlanningSchedulerTest {
    @Test
    void generatesAt0800And0810And0820SaoPaulo() throws Exception {
        var schedule = WeeklyPlanningScheduler.class.getMethod("generate").getAnnotation(Scheduled.class);

        assertEquals("0 0,10,20 8 * * *", schedule.cron());
        assertEquals("America/Sao_Paulo", schedule.zone());
    }

    @Test
    void confirmsOnceADayRightAfterMidnightSaoPaulo() throws Exception {
        var schedule = WeeklyPlanningScheduler.class.getMethod("confirm").getAnnotation(Scheduled.class);

        assertEquals("0 5 0 * * *", schedule.cron());
        assertEquals("America/Sao_Paulo", schedule.zone());
    }
}

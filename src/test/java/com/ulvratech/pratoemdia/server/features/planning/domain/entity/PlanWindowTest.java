package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlanWindowTest {
    // 2026-09-17 is a Thursday.
    private static final LocalDate THURSDAY = LocalDate.of(2026, 9, 17);

    @Test
    void automaticGenerationCoversTheDayAfterUpToTheNextAutomaticDayInclusive() {
        var window = PlanWindow.automatic(THURSDAY);

        assertEquals(LocalDate.of(2026, 9, 18), window.start());
        assertEquals(LocalDate.of(2026, 9, 24), window.end());
    }

    @Test
    void manualRegenerationCoversTodayUpToTheNextAutomaticDay() {
        var window = PlanWindow.manual(THURSDAY, DayOfWeek.MONDAY);

        assertEquals(THURSDAY, window.start());
        assertEquals(LocalDate.of(2026, 9, 21), window.end());
    }

    @Test
    void manualRegenerationOnTheAutomaticDayCoversOnlyThatDay() {
        var window = PlanWindow.manual(THURSDAY, DayOfWeek.THURSDAY);

        assertEquals(THURSDAY, window.start());
        assertEquals(THURSDAY, window.end());
    }

    @Test
    void manualRegenerationTheDayBeforeTheAutomaticDayCoversTwoDays() {
        var window = PlanWindow.manual(THURSDAY, DayOfWeek.FRIDAY);

        assertEquals(THURSDAY, window.start());
        assertEquals(LocalDate.of(2026, 9, 18), window.end());
    }
}

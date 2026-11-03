package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/** Intervalo de dias, inclusive nas duas pontas, que uma geração de plano cobre. */
public record PlanWindow(LocalDate start, LocalDate end) {
    public PlanWindow {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if (end.isBefore(start))
            throw new IllegalArgumentException("Plan window cannot end before it starts");
    }

    /** Geração automática no dia D: de D+1 até o próximo dia automático (D+7), inclusive. */
    public static PlanWindow automatic(LocalDate generatedOn) {
        return new PlanWindow(generatedOn.plusDays(1), generatedOn.plusDays(7));
    }

    /**
     * Regeneração manual: de hoje até o próximo dia automático; no próprio dia automático, só aquele dia.
     */
    public static PlanWindow manual(LocalDate today, DayOfWeek automaticDay) {
        if (today.getDayOfWeek() == automaticDay)
            return new PlanWindow(today, today);
        return new PlanWindow(today, today.with(TemporalAdjusters.next(automaticDay)));
    }
}

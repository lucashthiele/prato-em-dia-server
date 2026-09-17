package com.ulvratech.pratoemdia.server.features.planning.application.dto;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlanningPreference;
import java.time.DayOfWeek;
import java.util.UUID;

public record WeeklyPlanningPreferenceOutput(UUID patientId, DayOfWeek dayOfWeek, boolean enabled) {
    public static WeeklyPlanningPreferenceOutput from(WeeklyPlanningPreference value) {
        return new WeeklyPlanningPreferenceOutput(
                value.patientId(), value.dayOfWeek(), value.enabled()
        );
    }
}

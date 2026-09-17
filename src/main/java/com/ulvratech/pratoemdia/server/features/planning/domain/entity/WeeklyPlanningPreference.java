package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.UUID;

public record WeeklyPlanningPreference(UUID patientId, DayOfWeek dayOfWeek, boolean enabled) {
  public WeeklyPlanningPreference {
    Objects.requireNonNull(patientId, "Patient is required");
    Objects.requireNonNull(dayOfWeek, "Day of week is required");
  }
}

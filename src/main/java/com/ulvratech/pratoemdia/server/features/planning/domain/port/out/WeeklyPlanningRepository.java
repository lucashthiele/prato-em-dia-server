package com.ulvratech.pratoemdia.server.features.planning.domain.port.out;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlanningPreference;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyPlanningRepository {
    Optional<WeeklyPlanningPreference> findPreference(UUID patientId);

    WeeklyPlanningPreference savePreference(WeeklyPlanningPreference preference);

    List<WeeklyPlanningPreference> findEnabledPreferences(DayOfWeek dayOfWeek);

    Optional<WeeklyPlan> findPlan(UUID id);

    List<WeeklyPlan> findPlans(UUID patientId);

    boolean existsForPeriod(UUID patientId, LocalDate start, LocalDate end);

    WeeklyPlan savePlan(WeeklyPlan plan);

    List<WeeklyPlan> findPendingDue(Instant now);
}

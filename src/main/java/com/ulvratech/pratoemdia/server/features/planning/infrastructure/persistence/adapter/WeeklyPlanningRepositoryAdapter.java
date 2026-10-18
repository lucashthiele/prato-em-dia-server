package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlanningPreference;
import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.planning.domain.port.out.WeeklyPlanningRepository;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.mapper.WeeklyPlanningMapper;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class WeeklyPlanningRepositoryAdapter implements WeeklyPlanningRepository {
    private final WeeklyPlanJpaRepository plans;
    private final WeeklyPlanningPreferenceJpaRepository preferences;
    private final WeeklyPlanningMapper mapper;

    public WeeklyPlanningRepositoryAdapter(
            WeeklyPlanJpaRepository plans,
            WeeklyPlanningPreferenceJpaRepository preferences,
            WeeklyPlanningMapper mapper
    ) {
        this.plans = plans;
        this.preferences = preferences;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WeeklyPlanningPreference> findPreference(UUID id) {
        return preferences.findById(id).map(mapper::toDomain);
    }

    @Override
    public WeeklyPlanningPreference savePreference(WeeklyPlanningPreference value) {
        return mapper.toDomain(preferences.saveAndFlush(mapper.toModel(value)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyPlanningPreference> findEnabledPreferences(DayOfWeek day) {
        return preferences.findAllByEnabledTrueAndDayOfWeek(day.getValue()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WeeklyPlan> findPlan(UUID id) {
        return plans.findByIdAndActiveTrue(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyPlan> findPlans(UUID patientId) {
        return plans.findAllByPatientIdAndActiveTrueOrderByPeriodStartDesc(patientId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsForPeriod(UUID patientId, LocalDate start, LocalDate end) {
        return plans.existsByPatientIdAndPeriodStartAndPeriodEndAndActiveTrue(patientId, start, end);
    }

    @Override
    public WeeklyPlan savePlan(WeeklyPlan plan) {
        return mapper.toDomain(plans.saveAndFlush(mapper.toModel(plan)));
    }

    @Override
    public void deactivatePlan(UUID planId) {
        plans.deactivate(planId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyPlan> findPendingDue(Instant now) {
        return plans
                .findAllByStatusAndActiveTrueAndConfirmationDeadlineLessThanEqual(WeeklyPlanStatus.PENDING, now)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

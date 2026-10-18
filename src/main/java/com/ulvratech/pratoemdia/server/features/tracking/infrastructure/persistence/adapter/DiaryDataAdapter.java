package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter.PrescriptionQueryAdapter;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.NutrientTotal;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.PlanPeriod;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.PrescriptionPeriod;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.DiaryData;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DiaryDataAdapter implements DiaryData {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final ProfileQueryAdapter profiles;
    private final PlanningQueryAdapter planning;
    private final PrescriptionQueryAdapter prescriptions;
    private final MealExecutionJpaRepository executions;

    DiaryDataAdapter(
            ProfileQueryAdapter profiles, PlanningQueryAdapter planning,
            PrescriptionQueryAdapter prescriptions, MealExecutionJpaRepository executions
    ) {
        this.profiles = profiles;
        this.planning = planning;
        this.prescriptions = prescriptions;
        this.executions = executions;
    }

    @Override
    public Optional<UUID> patientId(UUID accountId) {
        return profiles.patientProfileId(accountId);
    }

    @Override
    public Calendar calendar(UUID patientId) {
        var plans = planning.planPeriods(patientId).stream()
                .map(plan -> new PlanPeriod(plan.planId(), plan.versionId(), plan.start(), plan.end()))
                .toList();
        var periods = prescriptions.publishedPeriods(patientId).stream()
                .map(
                        period -> new PrescriptionPeriod(
                                period.prescriptionId(), period.versionId(),
                                localDate(period.publishedAt()), localDate(period.archivedAt())
                        )
                ).toList();
        return new Calendar(plans, periods, executions.findFreeExecutionDates(patientId));
    }

    @Override
    public DailySummary dailySummary(UUID patientId, LocalDate date) {
        var context = planning.planCovering(patientId, date).orElse(null);
        Map<String, NutrientTotal> targets = context == null ? Map.of()
                : targets(context.versionId());
        Map<String, NutrientTotal> planned = context == null ? Map.of()
                : planned(context.planId(), date);
        Map<String, NutrientTotal> realized = new HashMap<>();
        executions.sumNutrients(patientId, date).forEach(
                row -> realized.put(row.getCode(), new NutrientTotal(row.getAmount(), row.getUnit()))
        );
        if (context == null)
            return new DailySummary(date, null, null, Map.of(), Map.of(), realized);
        Set<String> codes = new HashSet<>(targets.keySet());
        codes.addAll(planned.keySet());
        codes.addAll(realized.keySet());
        var prescribedFilled = new HashMap<>(targets);
        var plannedFilled = new HashMap<>(planned);
        var realizedFilled = new HashMap<>(realized);
        for (String code : codes) {
            String unit = targets.containsKey(code) ? targets.get(code).unit()
                    : planned.containsKey(code) ? planned.get(code).unit() : realized.get(code).unit();
            prescribedFilled.putIfAbsent(code, new NutrientTotal(BigDecimal.ZERO, unit));
            plannedFilled.putIfAbsent(code, new NutrientTotal(BigDecimal.ZERO, unit));
            realizedFilled.putIfAbsent(code, new NutrientTotal(BigDecimal.ZERO, unit));
        }
        return new DailySummary(
                date, context.planId(), context.versionId(),
                prescribedFilled, plannedFilled, realizedFilled
        );
    }

    private Map<String, NutrientTotal> targets(UUID versionId) {
        var result = new HashMap<String, NutrientTotal>();
        prescriptions.nutritionTargets(versionId).forEach(
                target -> result.put(
                        target.nutrientCode(), new NutrientTotal(target.amount(), target.unit())
                )
        );
        return result;
    }

    private Map<String, NutrientTotal> planned(UUID planId, LocalDate date) {
        var result = new HashMap<String, NutrientTotal>();
        planning.plannedNutrients(planId, date).forEach(
                row -> result.put(row.code(), new NutrientTotal(row.amount(), row.unit()))
        );
        return result;
    }

    private static LocalDate localDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZONE).toLocalDate();
    }
}

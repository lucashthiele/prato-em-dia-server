package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.NutritionTargetModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescriptionVersionModel;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.NutrientTotal;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.PlanPeriod;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.PrescriptionPeriod;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.DiaryData;
import jakarta.persistence.EntityManager;
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

    private final EntityManager entityManager;
    private final MealExecutionJpaRepository executions;

    DiaryDataAdapter(EntityManager entityManager, MealExecutionJpaRepository executions) {
        this.entityManager = entityManager;
        this.executions = executions;
    }

    @Override
    public Optional<UUID> patientId(UUID accountId) {
        return entityManager.createQuery(
                "select p.id from PatientProfileModel p where p.userAccountId = :account", UUID.class
        ).setParameter("account", accountId).getResultStream().findFirst();
    }

    @Override
    public Calendar calendar(UUID patientId) {
        var plans = entityManager.createQuery(
                "select p from WeeklyPlanModel p where p.patientId = :patient order by p.periodStart",
                WeeklyPlanModel.class
        ).setParameter("patient", patientId).getResultList().stream()
                .map(
                        plan -> new PlanPeriod(
                                plan.getId(), plan.getPrescriptionVersionId(), plan.getPeriodStart(),
                                plan.getPeriodEnd()
                        )
                ).toList();
        var prescriptions = entityManager.createQuery(
                """
                        select v from PrescriptionVersionModel v
                        where v.prescription.patientId = :patient and v.publishedAt is not null
                        order by v.publishedAt
                        """, PrescriptionVersionModel.class
        ).setParameter("patient", patientId).getResultList().stream()
                .map(
                        version -> new PrescriptionPeriod(
                                version.getPrescription().getId(), version.getId(),
                                localDate(version.getPublishedAt()), localDate(version.getArchivedAt())
                        )
                ).toList();
        return new Calendar(plans, prescriptions, executions.findFreeExecutionDates(patientId));
    }

    @Override
    public DailySummary dailySummary(UUID patientId, LocalDate date) {
        var context = entityManager.createQuery(
                """
                        select p from WeeklyPlanModel p
                        where p.patientId = :patient and :date between p.periodStart and p.periodEnd
                        order by p.periodStart desc
                        """, WeeklyPlanModel.class
        ).setParameter("patient", patientId).setParameter("date", date)
                .setMaxResults(1).getResultStream().findFirst().orElse(null);
        Map<String, NutrientTotal> targets = context == null ? Map.of()
                : targets(context.getPrescriptionVersionId());
        Map<String, NutrientTotal> planned = context == null ? Map.of()
                : planned(context.getId(), date);
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
                date, context.getId(), context.getPrescriptionVersionId(),
                prescribedFilled, plannedFilled, realizedFilled
        );
    }

    private Map<String, NutrientTotal> targets(UUID versionId) {
        var result = new HashMap<String, NutrientTotal>();
        entityManager.createQuery(
                "select t from NutritionTargetModel t where t.version.id = :version",
                NutritionTargetModel.class
        ).setParameter("version", versionId).getResultList().forEach(
                target -> result.put(
                        target.getNutrientCode(), new NutrientTotal(target.getAmount(), target.getUnit())
                )
        );
        return result;
    }

    private Map<String, NutrientTotal> planned(UUID planId, LocalDate date) {
        var result = new HashMap<String, NutrientTotal>();
        entityManager.createQuery(
                """
                        select s.nutrientCode, sum(s.amount), s.unit
                        from PlannedMealItemNutrientSnapshotModel s, PlannedMealItemModel i
                        where i.id = s.plannedItemId and i.meal.plan.id = :plan and i.meal.date = :date
                        group by s.nutrientCode, s.unit
                        """, Object[].class
        ).setParameter("plan", planId).setParameter("date", date).getResultList().forEach(
                row -> result.put(
                        (String) row[0], new NutrientTotal((BigDecimal) row[1], (String) row[2])
                )
        );
        return result;
    }

    private static LocalDate localDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZONE).toLocalDate();
    }
}

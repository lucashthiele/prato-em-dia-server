package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.PlanPeriod;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.PrescriptionPeriod;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.NutrientTotal;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.DiaryData;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DiaryDataAdapter implements DiaryData {
    private final JdbcTemplate jdbc;

    public DiaryDataAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> patientId(UUID accountId) {
        return jdbc.query(
                "select id from patient_profile where user_account_id=?",
                (rs, row) -> rs.getObject(1, UUID.class), accountId
        ).stream().findFirst();
    }

    @Override
    public Calendar calendar(UUID patientId) {
        var plans = jdbc.query(
                "select id,prescription_version_id,period_start,period_end "
                        + "from weekly_plan where patient_id=? order by period_start",
                (rs, row) -> new PlanPeriod(
                        rs.getObject(1, UUID.class),
                        rs.getObject(2, UUID.class), rs.getObject(3, LocalDate.class),
                        rs.getObject(4, LocalDate.class)
                ), patientId
        );
        var prescriptions = jdbc.query(
                "select p.id,v.id,(v.published_at at time zone 'America/Sao_Paulo')::date, "
                        + "(v.archived_at at time zone 'America/Sao_Paulo')::date "
                        + "from prescription p join prescription_version v on v.prescription_id=p.id "
                        + "where p.patient_id=? and v.published_at is not null order by v.published_at",
                (rs, row) -> new PrescriptionPeriod(
                        rs.getObject(1, UUID.class),
                        rs.getObject(2, UUID.class), rs.getObject(3, LocalDate.class),
                        rs.getObject(4, LocalDate.class)
                ), patientId
        );
        List<LocalDate> freeDates = jdbc.query(
                "select distinct execution_date from meal_execution "
                        + "where patient_id=? and planned_meal_id is null and deleted_at is null order by execution_date",
                (rs, row) -> rs.getObject(1, LocalDate.class), patientId
        );
        return new Calendar(plans, prescriptions, freeDates);
    }

    @Override
    public DailySummary dailySummary(UUID patientId, LocalDate date) {
        var context = jdbc.query(
                "select id,prescription_version_id from weekly_plan "
                        + "where patient_id=? and ? between period_start and period_end order by period_start desc limit 1",
                (rs, row) -> new UUID[] { rs.getObject(1, UUID.class), rs.getObject(2, UUID.class) },
                patientId, date
        ).stream().findFirst().orElse(null);
        Map<String, NutrientTotal> targets = context == null ? Map.of()
                : nutrients(
                        "select nutrient_code,amount,unit from prescription_nutrition_target where prescription_version_id=?",
                        context[1]
                );
        Map<String, NutrientTotal> planned = context == null ? Map.of()
                : nutrients(
                        "select s.nutrient_code,sum(s.amount),s.unit "
                                + "from planned_meal_item_nutrient_snapshot s "
                                + "join planned_meal_item i on i.id=s.planned_item_id "
                                + "join planned_meal m on m.id=i.planned_meal_id "
                                + "where m.weekly_plan_id=? and m.meal_date=? "
                                + "group by s.nutrient_code,s.unit",
                        context[0], date
                );
        Map<String, NutrientTotal> realized = nutrients(
                "select s.nutrient_code,sum(s.amount),s.unit "
                        + "from meal_execution_nutrient_snapshot s "
                        + "join meal_execution_item i on i.id=s.item_id "
                        + "join meal_execution e on e.id=i.execution_id "
                        + "where e.patient_id=? and e.execution_date=? and e.deleted_at is null "
                        + "group by s.nutrient_code,s.unit",
                patientId, date
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
                date, context[0], context[1],
                prescribedFilled, plannedFilled, realizedFilled
        );
    }

    private Map<String, NutrientTotal> nutrients(String sql, Object... parameters) {
        var result = new HashMap<String, NutrientTotal>();
        jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            result.put(rs.getString(1), new NutrientTotal(rs.getBigDecimal(2), rs.getString(3)));
        }, parameters);
        return result;
    }
}

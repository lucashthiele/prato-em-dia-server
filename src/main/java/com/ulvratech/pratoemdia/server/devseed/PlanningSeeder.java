package com.ulvratech.pratoemdia.server.devseed;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Seeds weekly_planning_preference, weekly_plan, planned_meal and planned_meal_item. The nutrient snapshots of
 * planned items are filled by the existing trigger.
 */
class PlanningSeeder {
    /** weekOffset is relative to the current week (Monday to Sunday). */
    record Plan(SeedAccount patient, int versionNumber, String mode, int weekOffset, String status) {
        UUID id() {
            return SeedIds.of("plan:" + patient.key() + ":" + weekOffset);
        }
    }

    static final List<Plan> PLANS = List.of(
            new Plan(SeedAccount.CARLA, 2, "RESTRICTED", -1, "CONFIRMED"),
            new Plan(SeedAccount.CARLA, 2, "RESTRICTED", 0, "CONFIRMED"),
            new Plan(SeedAccount.CARLA, 2, "RESTRICTED", 1, "PENDING"),
            new Plan(SeedAccount.DIEGO, 1, "SUBSTITUTIONS_ONLY", -1, "CONFIRMED"),
            new Plan(SeedAccount.DIEGO, 1, "SUBSTITUTIONS_ONLY", 0, "CONFIRMED")
    );

    private final JdbcTemplate jdbc;

    PlanningSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void seed(LocalDate today) {
        var thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        seedPreference(SeedAccount.CARLA, 5);
        seedPreference(SeedAccount.DIEGO, 6);
        for (var plan : PLANS) {
            seedPlan(plan, thisMonday.plusWeeks(plan.weekOffset()));
        }
    }

    private void seedPreference(SeedAccount patient, int dayOfWeek) {
        jdbc.update("""
                INSERT INTO weekly_planning_preference (patient_id, day_of_week)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, patient.profileId(), dayOfWeek);
    }

    private void seedPlan(Plan plan, LocalDate start) {
        var planId = plan.id();
        var versionId = PrescriptionSeeder.versionId(plan.patient(), plan.versionNumber());
        var deadline = Timestamp.from(start.minusDays(1).atTime(20, 0).toInstant(ZoneOffset.UTC));
        var confirmedAt = plan.status().equals("CONFIRMED")
                ? Timestamp.from(start.minusDays(2).atTime(18, 0).toInstant(ZoneOffset.UTC))
                : null;
        jdbc.update(
                """
                        INSERT INTO weekly_plan (
                            id, patient_id, prescription_version_id, planning_mode, period_start, period_end,
                            confirmation_deadline, status, confirmed_at
                        )
                        VALUES (?, ?, ?, CAST(? AS planning_mode), ?, ?, ?, CAST(? AS weekly_plan_status), ?)
                        ON CONFLICT DO NOTHING
                        """, planId, plan.patient().profileId(), versionId, plan.mode(), start, start.plusDays(6),
                deadline, plan.status(), confirmedAt
        );
        jdbc.update("""
                INSERT INTO planned_meal (
                    id, weekly_plan_id, prescribed_meal_id, meal_date, meal_time, meal_type, name, meal_order
                )
                SELECT md5(? || ':' || d || ':' || pm.id)::uuid, ?, pm.id, CAST(? AS date) + d,
                       pm.suggested_time, pm.meal_type, pm.name, pm.meal_order
                FROM prescribed_meal pm
                CROSS JOIN generate_series(0, 6) AS d
                WHERE pm.prescription_version_id = ?
                ON CONFLICT DO NOTHING
                """, planId.toString(), planId, start, versionId);
        jdbc.update("""
                INSERT INTO planned_meal_item (
                    id, planned_meal_id, prescribed_meal_item_id, food_id, food_portion_id, quantity, unit, item_order
                )
                SELECT md5(planned.id || ':' || item.id)::uuid, planned.id, item.id, item.food_id,
                       item.food_portion_id, item.quantity, item.unit, item.item_order
                FROM planned_meal planned
                JOIN prescribed_meal_item item ON item.prescribed_meal_id = planned.prescribed_meal_id
                WHERE planned.weekly_plan_id = ?
                ON CONFLICT DO NOTHING
                """, planId);
        if (plan.mode().equals("SUBSTITUTIONS_ONLY")) {
            // Wednesdays use the allowed substitution, so substituted items show up in the plan and diary.
            jdbc.update("""
                    UPDATE planned_meal_item planned_item
                    SET allowed_substitution_id = substitution.id,
                        food_id = substitution.substitute_food_id,
                        food_portion_id = substitution.substitute_food_portion_id,
                        quantity = substitution.quantity,
                        unit = substitution.unit
                    FROM allowed_substitution substitution, planned_meal planned
                    WHERE substitution.prescribed_meal_item_id = planned_item.prescribed_meal_item_id
                      AND planned.id = planned_item.planned_meal_id
                      AND planned.weekly_plan_id = ?
                      AND extract(isodow FROM planned.meal_date) = 3
                    """, planId);
        }
    }
}

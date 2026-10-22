package com.ulvratech.pratoemdia.server.devseed;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds meal_execution, meal_execution_item, meal_execution_nutrient_snapshot and meal_execution_command:
 * planned executions for confirmed weekly plans, free executions for the FREE patient and one soft-deleted row.
 */
class TrackingSeeder {
    private record FreeMeal(int daysAgo, String type, String time, int[][] items) {
    }

    /** items: {food slot, quantity}. */
    private static final List<FreeMeal> FREE_MEALS = List.of(
            new FreeMeal(1, "BREAKFAST", "07:45", new int[][] { { 0, 60 }, { 1, 200 } }),
            new FreeMeal(1, "LUNCH", "12:45", new int[][] { { 3, 150 }, { 4, 100 }, { 5, 150 } }),
            new FreeMeal(2, "BREAKFAST", "08:10", new int[][] { { 2, 120 }, { 7, 170 } }),
            new FreeMeal(2, "DINNER", "20:00", new int[][] { { 9, 200 }, { 10, 120 } }),
            new FreeMeal(3, "LUNCH", "13:00", new int[][] { { 3, 100 }, { 5, 180 }, { 6, 60 } })
    );

    private final JdbcTemplate jdbc;
    private final SeedFoods foods;

    TrackingSeeder(JdbcTemplate jdbc, SeedFoods foods) {
        this.jdbc = jdbc;
        this.foods = foods;
    }

    void seed(LocalDate today) {
        for (var plan : PlanningSeeder.PLANS) {
            if (plan.status().equals("CONFIRMED")) {
                seedPlannedExecutions(plan, today);
            }
        }
        seedFreeExecutions(today);
        softDeleteOneCarlaExecution();
    }

    private void seedPlannedExecutions(PlanningSeeder.Plan plan, LocalDate today) {
        var planId = plan.id();
        // Past meals are logged, except Tuesday's dinner, which stays missed. Today only has breakfast.
        jdbc.update("""
                INSERT INTO meal_execution (
                    id, patient_id, execution_date, planned_meal_id, weekly_plan_id, prescription_version_id, mode,
                    created_at, updated_at, meal_time, meal_type
                )
                SELECT md5('execution:' || planned.id)::uuid, plan.patient_id, planned.meal_date, planned.id, plan.id,
                       plan.prescription_version_id, plan.planning_mode,
                       (planned.meal_date + COALESCE(planned.meal_time, TIME '12:00')) AT TIME ZONE 'UTC',
                       (planned.meal_date + COALESCE(planned.meal_time, TIME '12:00')) AT TIME ZONE 'UTC',
                       COALESCE(planned.meal_time, TIME '12:00'), planned.meal_type
                FROM planned_meal planned
                JOIN weekly_plan plan ON plan.id = planned.weekly_plan_id
                WHERE plan.id = ?
                  AND (planned.meal_date < CAST(? AS date)
                       OR (planned.meal_date = CAST(? AS date) AND planned.meal_order = 0))
                  AND NOT (planned.meal_type = 'DINNER' AND extract(isodow FROM planned.meal_date) = 2)
                ON CONFLICT DO NOTHING
                """, planId, today, today);
        jdbc.update("""
                INSERT INTO meal_execution_item (
                    id, execution_id, item_order, planned_item_id, allowed_substitution_id, food_id, food_portion_id,
                    quantity, multiplier, unit, grams, kcal
                )
                SELECT md5('execution-item:' || item.id)::uuid, execution.id, item.item_order, item.id,
                       item.allowed_substitution_id, item.food_id, item.food_portion_id, item.quantity, 1, item.unit,
                       ROUND(portion.grams_equivalent * item.quantity / portion.quantity, 4),
                       (SELECT snapshot.amount FROM planned_meal_item_nutrient_snapshot snapshot
                        WHERE snapshot.planned_item_id = item.id AND snapshot.nutrient_code = 'energy_kcal')
                FROM meal_execution execution
                JOIN planned_meal_item item ON item.planned_meal_id = execution.planned_meal_id
                JOIN food_portion portion ON portion.id = item.food_portion_id
                WHERE execution.weekly_plan_id = ?
                ON CONFLICT DO NOTHING
                """, planId);
        jdbc.update(
                """
                        INSERT INTO meal_execution_nutrient_snapshot (item_id, nutrient_code, amount, unit)
                        SELECT execution_item.id, snapshot.nutrient_code, snapshot.amount, snapshot.unit
                        FROM meal_execution execution
                        JOIN meal_execution_item execution_item ON execution_item.execution_id = execution.id
                        JOIN planned_meal_item_nutrient_snapshot snapshot ON snapshot.planned_item_id = execution_item.planned_item_id
                        WHERE execution.weekly_plan_id = ?
                        ON CONFLICT DO NOTHING
                        """,
                planId
        );
        jdbc.update(
                """
                        INSERT INTO meal_execution_command (patient_id, idempotency_key, command_hash, execution_id, result_version)
                        SELECT execution.patient_id, 'dev-seed-' || execution.id,
                               encode(sha256(convert_to('dev-seed-' || execution.id, 'UTF8')), 'hex'), execution.id, 1
                        FROM meal_execution execution
                        WHERE execution.weekly_plan_id = ?
                        ORDER BY execution.execution_date, execution.meal_time
                        LIMIT 3
                        ON CONFLICT DO NOTHING
                        """,
                planId
        );
    }

    private void seedFreeExecutions(LocalDate today) {
        var patient = SeedAccount.ELISA;
        var versionId = PrescriptionSeeder.versionId(patient, 1);
        for (var meal : FREE_MEALS) {
            var date = today.minusDays(meal.daysAgo());
            var executionId = SeedIds.of("execution:free:" + patient.key() + ":" + date + ":" + meal.type());
            jdbc.update(
                    """
                            INSERT INTO meal_execution (
                                id, patient_id, execution_date, prescription_version_id, mode, created_at, updated_at,
                                meal_time, meal_type
                            )
                            VALUES (?, ?, ?, ?, 'FREE', (CAST(? AS date) + CAST(? AS time)) AT TIME ZONE 'UTC',
                                    (CAST(? AS date) + CAST(? AS time)) AT TIME ZONE 'UTC', CAST(? AS time),
                                    CAST(? AS meal_type))
                            ON CONFLICT DO NOTHING
                            """, executionId, patient.profileId(), date, versionId, date, meal.time(), date,
                    meal.time(),
                    meal.time(), meal.type()
            );
            for (int order = 0; order < meal.items().length; order++) {
                var food = foods.get(meal.items()[order][0]);
                var itemId = SeedIds.of("execution-item:" + executionId + ":" + order);
                var quantity = meal.items()[order][1];
                jdbc.update(
                        """
                                INSERT INTO meal_execution_item (
                                    id, execution_id, item_order, food_id, food_portion_id, quantity, multiplier, unit, grams, kcal
                                )
                                SELECT ?, ?, ?, ?, portion.id, ?, 1, portion.unit,
                                       ROUND(portion.grams_equivalent * ? / portion.quantity, 4),
                                       (SELECT ROUND(nutrient.amount * ? / portion.quantity, 4) FROM food_portion_nutrient nutrient
                                        WHERE nutrient.food_portion_id = portion.id AND nutrient.nutrient_code = 'energy_kcal')
                                FROM food_portion portion
                                WHERE portion.id = ?
                                ON CONFLICT DO NOTHING
                                """,
                        itemId, executionId, order, food.foodId(), quantity, quantity, quantity,
                        food.portionId()
                );
                jdbc.update(
                        """
                                INSERT INTO meal_execution_nutrient_snapshot (item_id, nutrient_code, amount, unit)
                                SELECT ?, nutrient.nutrient_code, ROUND(nutrient.amount * ? / portion.quantity, 4), nutrient.unit
                                FROM food_portion portion
                                JOIN food_portion_nutrient nutrient ON nutrient.food_portion_id = portion.id
                                WHERE portion.id = ? AND nutrient.amount IS NOT NULL
                                ON CONFLICT DO NOTHING
                                """,
                        itemId, quantity, food.portionId()
                );
            }
        }
    }

    private void softDeleteOneCarlaExecution() {
        jdbc.update("""
                UPDATE meal_execution
                SET deleted_at = now(), restore_token = ?, restore_expires_at = now() + interval '1 day'
                WHERE id = (
                    SELECT id FROM meal_execution
                    WHERE patient_id = ? AND planned_meal_id IS NOT NULL AND deleted_at IS NULL AND meal_type = 'SNACK'
                    ORDER BY execution_date DESC
                    LIMIT 1
                )
                """, SeedIds.of("restore-token:" + SeedAccount.CARLA.key()), SeedAccount.CARLA.profileId());
    }
}

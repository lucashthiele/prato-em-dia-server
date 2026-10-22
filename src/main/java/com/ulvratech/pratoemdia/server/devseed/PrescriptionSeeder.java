package com.ulvratech.pratoemdia.server.devseed;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Seeds prescription, prescription_version, prescribed_meal, prescribed_meal_item, allowed_substitution and
 * prescription_nutrition_target. Versions are inserted as DRAFT and promoted afterwards because the
 * immutability triggers reject content written under a finalized version.
 */
class PrescriptionSeeder {
    record Version(int number, String status, String title) {
    }

    record Spec(
            SeedAccount nutritionist,
            SeedAccount patient,
            String planningMode,
            String status,
            boolean substitutions,
            List<Version> versions) {
    }

    private record MealSpec(String type, String name, String time, int[][] items) {
    }

    /** items: {food slot, quantity}. Slots match {@link SeedFoods}. */
    private static final List<MealSpec> MEALS = List.of(
            new MealSpec("BREAKFAST", "Café da manhã", "07:30", new int[][] { { 0, 50 }, { 1, 200 }, { 2, 100 } }),
            new MealSpec("LUNCH", "Almoço", "12:30", new int[][] { { 3, 120 }, { 4, 100 }, { 5, 120 }, { 6, 80 } }),
            new MealSpec("SNACK", "Lanche da tarde", "16:00", new int[][] { { 7, 150 }, { 8, 100 } }),
            new MealSpec("DINNER", "Jantar", "19:30", new int[][] { { 9, 150 }, { 10, 100 }, { 11, 80 } })
    );

    /** meal index, item index -> substitute food slot. */
    private static final int[][] SUBSTITUTIONS = { { 0, 0, 12 }, { 1, 0, 13 }, { 1, 2, 14 } };

    private static final Object[][] TARGETS = {
            { "energy_kcal", 1800, "kcal" },
            { "protein", 90, "g" },
            { "carbohydrate", 240, "g" },
            { "lipids", 60, "g" },
            { "dietary_fiber", 28, "g" }
    };

    static final List<Spec> SPECS = List.of(
            new Spec(
                    SeedAccount.ANA, SeedAccount.CARLA, "RESTRICTED", "ACTIVE", false, List.of(
                            new Version(1, "ARCHIVED", "Plano inicial"),
                            new Version(2, "PUBLISHED", "Plano de manutenção"),
                            new Version(3, "DRAFT", "Ajuste em rascunho")
                    )
            ),
            new Spec(
                    SeedAccount.ANA, SeedAccount.DIEGO, "SUBSTITUTIONS_ONLY", "ACTIVE", true, List.of(
                            new Version(1, "PUBLISHED", "Plano com substituições")
                    )
            ),
            new Spec(
                    SeedAccount.BRUNO, SeedAccount.ELISA, "FREE", "ACTIVE", false, List.of(
                            new Version(1, "PUBLISHED", "Plano livre")
                    )
            ),
            new Spec(
                    SeedAccount.ANA, SeedAccount.FABIO, "RESTRICTED", "ACTIVE", false, List.of(
                            new Version(1, "DRAFT", "Primeira prescrição")
                    )
            ),
            new Spec(
                    SeedAccount.BRUNO, SeedAccount.GABI, "RESTRICTED", "ARCHIVED", false, List.of(
                            new Version(1, "ARCHIVED", "Plano encerrado")
                    )
            )
    );

    static UUID versionId(SeedAccount patient, int number) {
        return SeedIds.of("version:" + patient.key() + ":" + number);
    }

    private final JdbcTemplate jdbc;
    private final SeedFoods foods;

    PrescriptionSeeder(JdbcTemplate jdbc, SeedFoods foods) {
        this.jdbc = jdbc;
        this.foods = foods;
    }

    void seed() {
        for (var spec : SPECS) {
            var prescriptionId = SeedIds.of("prescription:" + spec.patient().key());
            jdbc.update(
                    """
                            INSERT INTO prescription (id, patient_id, nutritionist_id, status, planning_mode)
                            VALUES (?, ?, ?, CAST(? AS prescription_status), CAST(? AS planning_mode))
                            ON CONFLICT DO NOTHING
                            """, prescriptionId, spec.patient().profileId(), spec.nutritionist().profileId(),
                    spec.status(), spec.planningMode()
            );
            for (var version : spec.versions()) {
                seedVersion(spec, prescriptionId, version);
            }
        }
    }

    private void seedVersion(Spec spec, UUID prescriptionId, Version version) {
        var id = versionId(spec.patient(), version.number());
        var createdAt = Instant.now().minus(60L - version.number() * 15L, ChronoUnit.DAYS);
        jdbc.update(
                """
                        INSERT INTO prescription_version (
                            id, prescription_id, version_number, status, title, notes, created_by_user_id, created_at, updated_at
                        )
                        VALUES (?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                        """,
                id, prescriptionId, version.number(), version.title(),
                "Prescrição de desenvolvimento.", spec.nutritionist().accountId(),
                java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt)
        );
        for (var target : TARGETS) {
            jdbc.update("""
                    INSERT INTO prescription_nutrition_target (id, prescription_version_id, nutrient_code, amount, unit)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT DO NOTHING
                    """, SeedIds.of("target:" + id + ":" + target[0]), id, target[0], target[1], target[2]);
        }
        seedMeals(spec, id);
        var finalizedAt = java.sql.Timestamp.from(createdAt.plus(1, ChronoUnit.DAYS));
        if (version.status().equals("PUBLISHED")) {
            jdbc.update(
                    "UPDATE prescription_version SET status = 'PUBLISHED', published_at = ? WHERE id = ?",
                    finalizedAt, id
            );
        } else if (version.status().equals("ARCHIVED")) {
            jdbc.update("""
                    UPDATE prescription_version SET status = 'ARCHIVED', published_at = ?, archived_at = ? WHERE id = ?
                    """, finalizedAt, java.sql.Timestamp.from(createdAt.plus(10, ChronoUnit.DAYS)), id);
        }
    }

    private void seedMeals(Spec spec, UUID versionId) {
        for (int mealIndex = 0; mealIndex < MEALS.size(); mealIndex++) {
            var meal = MEALS.get(mealIndex);
            var mealId = SeedIds.of("meal:" + versionId + ":" + meal.type());
            jdbc.update(
                    """
                            INSERT INTO prescribed_meal (id, prescription_version_id, name, meal_order, suggested_time, meal_type)
                            VALUES (?, ?, ?, ?, CAST(? AS time), CAST(? AS meal_type))
                            ON CONFLICT DO NOTHING
                            """,
                    mealId, versionId, meal.name(), mealIndex, meal.time(), meal.type()
            );
            for (int itemIndex = 0; itemIndex < meal.items().length; itemIndex++) {
                var food = foods.get(meal.items()[itemIndex][0]);
                var itemId = SeedIds.of("item:" + mealId + ":" + itemIndex);
                jdbc.update(
                        """
                                INSERT INTO prescribed_meal_item (
                                    id, prescribed_meal_id, food_id, food_portion_id, quantity, unit, item_order
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                ON CONFLICT DO NOTHING
                                """, itemId, mealId, food.foodId(), food.portionId(), meal.items()[itemIndex][1],
                        food.unit(), itemIndex
                );
                if (spec.substitutions()) {
                    seedSubstitutions(mealIndex, itemIndex, itemId);
                }
            }
        }
    }

    private void seedSubstitutions(int mealIndex, int itemIndex, UUID itemId) {
        for (var substitution : SUBSTITUTIONS) {
            if (substitution[0] != mealIndex || substitution[1] != itemIndex) {
                continue;
            }
            var food = foods.get(substitution[2]);
            var original = MEALS.get(mealIndex).items()[itemIndex][1];
            jdbc.update(
                    """
                            INSERT INTO allowed_substitution (
                                id, prescribed_meal_item_id, substitute_food_id, substitute_food_portion_id, quantity, unit
                            )
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON CONFLICT DO NOTHING
                            """,
                    SeedIds.of("substitution:" + itemId + ":" + substitution[2]), itemId, food.foodId(),
                    food.portionId(), original, food.unit()
            );
        }
    }
}

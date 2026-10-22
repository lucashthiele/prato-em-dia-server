package com.ulvratech.pratoemdia.server.devseed;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** Removes only what the seed created, found through the seed email domain. */
class SeedReset {
    private static final String ACCOUNTS = "(SELECT id FROM user_account WHERE email LIKE '%" + SeedAccount.EMAIL_DOMAIN
            + "')";
    private static final String PATIENTS = "(SELECT id FROM patient_profile WHERE user_account_id IN " + ACCOUNTS + ")";
    private static final String NUTRITIONISTS = "(SELECT id FROM nutritionist_profile WHERE user_account_id IN "
            + ACCOUNTS + ")";

    /** table, trigger: finalized prescription content is immutable, so deleting it needs these suspended. */
    private static final List<String[]> IMMUTABILITY_TRIGGERS = List.of(
            new String[] { "prescription_version", "trg_protect_published_prescription_version" },
            new String[] { "prescribed_meal", "trg_protect_published_meal" },
            new String[] { "prescribed_meal_item", "trg_protect_published_meal_item" },
            new String[] { "allowed_substitution", "trg_protect_published_substitution" },
            new String[] { "prescription_nutrition_target", "trg_protect_published_nutrition_target" }
    );

    private final JdbcTemplate jdbc;

    SeedReset(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void reset() {
        setTriggers("DISABLE");
        jdbc.update("DELETE FROM meal_execution_command WHERE patient_id IN " + PATIENTS);
        jdbc.update("DELETE FROM meal_execution WHERE patient_id IN " + PATIENTS);
        jdbc.update("DELETE FROM weekly_plan WHERE patient_id IN " + PATIENTS);
        jdbc.update("DELETE FROM weekly_planning_preference WHERE patient_id IN " + PATIENTS);
        jdbc.update(
                "DELETE FROM prescription WHERE patient_id IN " + PATIENTS
                        + " OR nutritionist_id IN " + NUTRITIONISTS
        );
        setTriggers("ENABLE");
        jdbc.update(
                "DELETE FROM nutritionist_patient_relationship WHERE patient_id IN " + PATIENTS
                        + " OR nutritionist_id IN " + NUTRITIONISTS
        );
        jdbc.update("DELETE FROM refresh_token WHERE user_account_id IN " + ACCOUNTS);
        jdbc.update("DELETE FROM patient_profile WHERE id IN " + PATIENTS);
        jdbc.update("DELETE FROM nutritionist_profile WHERE id IN " + NUTRITIONISTS);
        jdbc.update("DELETE FROM user_account WHERE email LIKE '%" + SeedAccount.EMAIL_DOMAIN + "'");
    }

    private void setTriggers(String action) {
        for (var trigger : IMMUTABILITY_TRIGGERS) {
            jdbc.execute("ALTER TABLE " + trigger[0] + " " + action + " TRIGGER " + trigger[1]);
        }
    }
}

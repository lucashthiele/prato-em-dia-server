package com.ulvratech.pratoemdia.server.support;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Builds a patient, a nutritionist, a prescription and a weekly plan directly in the database.
 * Foods have a single 100 g portion with 100 kcal and 10 g of protein.
 */
public final class TrackingFixture {
    public record Food(UUID foodId, UUID portionId) {
    }

    public record PrescribedItem(UUID id, Food food) {
    }

    public record PrescribedMeal(UUID id, String type, int order, List<PrescribedItem> items) {
    }

    public record PlannedItem(UUID id, UUID prescribedItemId, Food food) {
    }

    public record PlannedMeal(UUID id, LocalDate date, String type, List<PlannedItem> items) {
    }

    public static final LocalDate PERIOD_START = LocalDate.parse("2026-10-12");

    public final JdbcTemplate jdbc;
    public final String mode;
    public final UUID patientAccount = UUID.randomUUID();
    public final UUID patient = UUID.randomUUID();
    public final UUID nutritionistAccount = UUID.randomUUID();
    public final UUID nutritionist = UUID.randomUUID();
    public final UUID prescription = UUID.randomUUID();
    public final UUID version = UUID.randomUUID();
    public final UUID plan = UUID.randomUUID();
    private final List<PrescribedMeal> prescribedMeals = new ArrayList<>();
    private int mealOrder;
    private boolean published;

    public TrackingFixture(JdbcTemplate jdbc, String mode) {
        this.jdbc = jdbc;
        this.mode = mode;
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'PATIENT'::user_role)",
                patientAccount, patientAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                patient, patientAccount, "Patient"
        );
        jdbc.update(
                "insert into user_account(id,email,password_hash,role) values (?,?,?,'NUTRITIONIST'::user_role)",
                nutritionistAccount, nutritionistAccount + "@example.com", "hash"
        );
        jdbc.update(
                "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
                nutritionist, nutritionistAccount, "Nutritionist"
        );
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id,planning_mode) values (?,?,?,?::planning_mode)",
                prescription, patient, nutritionist, mode
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,1,'DRAFT',?)",
                version, prescription, nutritionistAccount
        );
    }

    public ExecutionTypes.Actor actor() {
        return new ExecutionTypes.Actor(patientAccount, "PATIENT");
    }

    public void link() {
        jdbc.update(
                "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id) values (?,?,?)",
                UUID.randomUUID(), nutritionist, patient
        );
    }

    public Food food(String name) {
        var foodId = UUID.randomUUID();
        var portionId = UUID.randomUUID();
        jdbc.update(
                "insert into food(id,name,default_unit,created_by_user_id,origin) values (?,?,?,?,'NUTRITIONIST'::food_origin)",
                foodId, name, "g", nutritionistAccount
        );
        jdbc.update(
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,100,'g')",
                portionId, foodId, name + " 100 g"
        );
        nutrient(portionId, "energy_kcal", "100", "kcal");
        nutrient(portionId, "protein", "10", "g");
        return new Food(foodId, portionId);
    }

    public void nutrient(UUID portionId, String code, String amount, String unit) {
        jdbc.update(
                "insert into food_portion_nutrient(id,food_portion_id,nutrient_code,amount,unit,value_status) values (?,?,?,?,?,'VALUE')",
                UUID.randomUUID(), portionId, code, new BigDecimal(amount), unit
        );
    }

    public PrescribedMeal prescribedMeal(String type, Food... foods) {
        var mealId = UUID.randomUUID();
        var order = mealOrder++;
        jdbc.update(
                "insert into prescribed_meal(id,prescription_version_id,name,meal_order,meal_type) values (?,?,?,?,?::meal_type)",
                mealId, version, type, order, type
        );
        var items = new ArrayList<PrescribedItem>();
        for (var food : foods) {
            var itemId = UUID.randomUUID();
            jdbc.update(
                    "insert into prescribed_meal_item(id,prescribed_meal_id,food_id,food_portion_id,quantity,unit,item_order) values (?,?,?,?,100,'g',?)",
                    itemId, mealId, food.foodId(), food.portionId(), items.size()
            );
            items.add(new PrescribedItem(itemId, food));
        }
        var meal = new PrescribedMeal(mealId, type, order, items);
        prescribedMeals.add(meal);
        return meal;
    }

    public void substitution(PrescribedItem item, Food substitute) {
        jdbc.update(
                "insert into allowed_substitution(id,prescribed_meal_item_id,substitute_food_id,substitute_food_portion_id,quantity,unit) values (?,?,?,?,100,'g')",
                UUID.randomUUID(), item.id(), substitute.foodId(), substitute.portionId()
        );
    }

    public UUID substitutionId(PrescribedItem item) {
        return jdbc.queryForObject(
                "select id from allowed_substitution where prescribed_meal_item_id=?", UUID.class,
                item.id()
        );
    }

    /** Publishes the version and creates the plan covering the fixed test week. */
    public void publish() {
        if (published)
            return;
        jdbc.update("update prescription_version set status='PUBLISHED',published_at=now() where id=?", version);
        jdbc.update(
                "insert into weekly_plan(id,patient_id,prescription_version_id,planning_mode,period_start,period_end,confirmation_deadline) values (?,?,?,?::planning_mode,?,?,now())",
                plan, patient, version, mode, PERIOD_START, PERIOD_START.plusDays(6)
        );
        published = true;
    }

    public PlannedMeal plannedMeal(LocalDate date, PrescribedMeal prescribed) {
        publish();
        var mealId = UUID.randomUUID();
        jdbc.update(
                "insert into planned_meal(id,weekly_plan_id,prescribed_meal_id,meal_date,meal_type,name,meal_order) values (?,?,?,?,?::meal_type,?,?)",
                mealId, plan, prescribed.id(), date, prescribed.type(), prescribed.type(), prescribed.order()
        );
        var items = new ArrayList<PlannedItem>();
        for (var item : prescribed.items()) {
            var itemId = UUID.randomUUID();
            jdbc.update(
                    "insert into planned_meal_item(id,planned_meal_id,prescribed_meal_item_id,food_id,food_portion_id,quantity,unit,item_order) values (?,?,?,?,?,100,'g',?)",
                    itemId, mealId, item.id(), item.food().foodId(), item.food().portionId(), items.size()
            );
            items.add(new PlannedItem(itemId, item.id(), item.food()));
        }
        return new PlannedMeal(mealId, date, prescribed.type(), items);
    }

    public static ExecutionTypes.ItemInput input(PlannedItem item) {
        return new ExecutionTypes.ItemInput(
                item.food().foodId(), item.food().portionId(), "g", new BigDecimal("100"),
                BigDecimal.ONE, item.id(), null
        );
    }

    public static ExecutionTypes.ItemInput free(Food food) {
        return new ExecutionTypes.ItemInput(
                food.foodId(), food.portionId(), "g", new BigDecimal("100"), BigDecimal.ONE, null,
                null
        );
    }

    public static ExecutionTypes.Write write(PlannedMeal meal, LocalTime time, ExecutionTypes.ItemInput... items) {
        return new ExecutionTypes.Write(meal.date(), time, null, meal.id(), List.of(items));
    }
}

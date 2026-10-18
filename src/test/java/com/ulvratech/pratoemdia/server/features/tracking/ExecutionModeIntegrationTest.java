package com.ulvratech.pratoemdia.server.features.tracking;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ExecutionModeIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ExecutionOperations executions;

    @Test
    void substitutionsOnlyRequiresTheAuthorizedPortion() {
        var patientAccount = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var nutritionistAccount = UUID.randomUUID();
        var nutritionist = UUID.randomUUID();
        var prescription = UUID.randomUUID();
        var version = UUID.randomUUID();
        var prescribedMeal = UUID.randomUUID();
        var prescribedItem = UUID.randomUUID();
        var substitution = UUID.randomUUID();
        var originalFood = UUID.randomUUID();
        var substituteFood = UUID.randomUUID();
        var originalPortion = UUID.randomUUID();
        var authorizedPortion = UUID.randomUUID();
        var otherPortion = UUID.randomUUID();
        var plan = UUID.randomUUID();
        var meal = UUID.randomUUID();
        var plannedItem = UUID.randomUUID();
        var date = LocalDate.parse("2026-10-13");
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
                "insert into food(id,name,default_unit,created_by_user_id,origin) values (?,?,?,?,'NUTRITIONIST'::food_origin)",
                originalFood, "Original", "g", nutritionistAccount
        );
        jdbc.update(
                "insert into food(id,name,default_unit,created_by_user_id,origin) values (?,?,?,?,'NUTRITIONIST'::food_origin)",
                substituteFood, "Substitute", "g", nutritionistAccount
        );
        jdbc.update(
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,100,'g')",
                originalPortion, originalFood, "Original 100 g"
        );
        jdbc.update(
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,100,'g')",
                authorizedPortion, substituteFood, "Authorized 100 g"
        );
        jdbc.update(
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,50,'g')",
                otherPortion, substituteFood, "Other 50 g"
        );
        jdbc.update(
                "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)",
                prescription, patient, nutritionist
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,1,'DRAFT',?)",
                version, prescription, nutritionistAccount
        );
        jdbc.update(
                "insert into prescribed_meal(id,prescription_version_id,name,meal_order,meal_type) values (?,?,?,0,'LUNCH'::meal_type)",
                prescribedMeal, version, "Lunch"
        );
        jdbc.update(
                "insert into prescribed_meal_item(id,prescribed_meal_id,food_id,food_portion_id,quantity,unit,item_order) values (?,?,?,?,100,'g',0)",
                prescribedItem, prescribedMeal, originalFood, originalPortion
        );
        jdbc.update(
                "insert into allowed_substitution(id,prescribed_meal_item_id,substitute_food_id,substitute_food_portion_id,quantity,unit) values (?,?,?,?,100,'g')",
                substitution, prescribedItem, substituteFood, authorizedPortion
        );
        jdbc.update("update prescription_version set status='PUBLISHED',published_at=now() where id=?", version);
        jdbc.update(
                "insert into weekly_plan(id,patient_id,prescription_version_id,planning_mode,period_start,period_end,confirmation_deadline) values (?,?,?,'SUBSTITUTIONS_ONLY','2026-10-12','2026-10-18',now())",
                plan, patient, version
        );
        jdbc.update(
                "insert into planned_meal(id,weekly_plan_id,prescribed_meal_id,meal_date,meal_type,name,meal_order) values (?,?,?,?,'LUNCH','Lunch',0)",
                meal, plan, prescribedMeal, date
        );
        jdbc.update(
                "insert into planned_meal_item(id,planned_meal_id,prescribed_meal_item_id,food_id,food_portion_id,quantity,unit,item_order) values (?,?,?,?,?,100,'g',0)",
                plannedItem, meal, prescribedItem, originalFood, originalPortion
        );
        var actor = new ExecutionTypes.Actor(patientAccount, "PATIENT");
        var wrong = new ExecutionTypes.ItemInput(
                substituteFood, otherPortion, "g",
                new BigDecimal("100"), BigDecimal.ONE, plannedItem, substitution
        );

        var error = assertThrows(
                TrackingException.class,
                () -> executions.create(
                        actor, "wrong-portion",
                        new ExecutionTypes.Write(date, java.time.LocalTime.NOON, null, meal, List.of(wrong))
                )
        );
        assertEquals(TrackingException.Kind.INVALID, error.kind());

        var correct = new ExecutionTypes.ItemInput(
                substituteFood, authorizedPortion, "g",
                new BigDecimal("100"), BigDecimal.ONE, plannedItem, substitution
        );
        assertEquals(
                "SUBSTITUTIONS_ONLY", executions.create(
                        actor, "authorized-substitution",
                        new ExecutionTypes.Write(date, java.time.LocalTime.NOON, null, meal, List.of(correct))
                ).mode()
        );

        var restrictedDate = date.plusDays(1);
        var restrictedMeal = UUID.randomUUID();
        var restrictedItem = UUID.randomUUID();
        jdbc.update("update weekly_plan set planning_mode='RESTRICTED' where id=?", plan);
        jdbc.update(
                "insert into planned_meal(id,weekly_plan_id,prescribed_meal_id,meal_date,meal_type,name,meal_order) values (?,?,?,?,'LUNCH','Lunch',0)",
                restrictedMeal, plan, prescribedMeal, restrictedDate
        );
        jdbc.update(
                "insert into planned_meal_item(id,planned_meal_id,prescribed_meal_item_id,food_id,food_portion_id,quantity,unit,item_order) values (?,?,?,?,?,100,'g',0)",
                restrictedItem, restrictedMeal, prescribedItem, originalFood, originalPortion
        );
        var unauthorized = new ExecutionTypes.ItemInput(
                substituteFood, authorizedPortion, "g",
                new BigDecimal("100"), BigDecimal.ONE, restrictedItem, substitution
        );
        var restrictedError = assertThrows(
                TrackingException.class,
                () -> executions.create(
                        actor, "restricted-substitution",
                        new ExecutionTypes.Write(
                                restrictedDate, java.time.LocalTime.NOON, null, restrictedMeal, List.of(unauthorized)
                        )
                )
        );
        assertEquals(TrackingException.Kind.INVALID, restrictedError.kind());
        var original = new ExecutionTypes.ItemInput(
                originalFood, originalPortion, "g",
                new BigDecimal("100"), BigDecimal.ONE, restrictedItem, null
        );
        assertEquals(
                "RESTRICTED", executions.create(
                        actor, "restricted-original",
                        new ExecutionTypes.Write(
                                restrictedDate, java.time.LocalTime.NOON, null, restrictedMeal, List.of(original)
                        )
                ).mode()
        );
    }
}

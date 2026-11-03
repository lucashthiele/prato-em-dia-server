package com.ulvratech.pratoemdia.server.features.prescriptions;

import com.ulvratech.pratoemdia.server.support.TrackingFixture;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class PrescriptionSummaryIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    MockMvc mvc;

    ResultActions summary(UUID nutritionistAccount, UUID patient) throws Exception {
        return mvc.perform(
                get("/api/v1/nutritionists/me/patients/" + patient + "/prescription-summary")
                        .with(
                                jwt().jwt(t -> t.subject(nutritionistAccount.toString()).claim("role", "NUTRITIONIST"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"))
                        )
        );
    }

    TrackingFixture.Food macroFood(TrackingFixture f, String name) {
        var food = f.food(name);
        f.nutrient(food.portionId(), "carbohydrate", "20", "g");
        f.nutrient(food.portionId(), "lipids", "5", "g");
        return food;
    }

    @Test
    void returnsAnEmptySummaryWhenThePatientHasNoPrescription() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        jdbc.update("delete from prescription_version where id=?", f.version);
        jdbc.update("delete from prescription where id=?", f.prescription);

        summary(f.nutritionistAccount, f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionId").doesNotExist())
                .andExpect(jsonPath("$.current").doesNotExist())
                .andExpect(jsonPath("$.draft").doesNotExist())
                .andExpect(jsonPath("$.history.length()").value(0));
    }

    @Test
    void returnsOnlyTheDraftWithFoodNamesAndMacros() throws Exception {
        var f = new TrackingFixture(jdbc, "SUBSTITUTIONS_ONLY");
        f.link();
        f.prescribedMeal("LUNCH", macroFood(f, "Rice"));

        summary(f.nutritionistAccount, f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionId").value(f.prescription.toString()))
                .andExpect(jsonPath("$.planningMode").value("SUBSTITUTIONS_ONLY"))
                .andExpect(jsonPath("$.current").doesNotExist())
                .andExpect(jsonPath("$.draft.id").value(f.version.toString()))
                .andExpect(jsonPath("$.draft.meals[0].items[0].foodName").value("Rice"))
                .andExpect(jsonPath("$.draft.meals[0].items[0].portionLabel").value("Rice 100 g"))
                .andExpect(jsonPath("$.draft.meals[0].items[0].nutrients.energyKcal").value(100.0))
                .andExpect(jsonPath("$.draft.meals[0].items[0].nutrients.protein").value(10.0))
                .andExpect(jsonPath("$.draft.meals[0].items[0].nutrients.complete").value(true))
                .andExpect(jsonPath("$.draft.totals.energyKcal").value(100.0))
                .andExpect(jsonPath("$.draft.totals.complete").value(true))
                .andExpect(jsonPath("$.history.length()").value(1));
    }

    @Test
    void returnsPublishedDraftAndArchivedVersions() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        f.prescribedMeal("LUNCH", macroFood(f, "Rice"));
        f.publish();
        var draft = UUID.randomUUID();
        var archived = UUID.randomUUID();
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id) values (?,?,2,'DRAFT',?)",
                draft, f.prescription, f.nutritionistAccount
        );
        jdbc.update(
                "insert into prescription_version(id,prescription_id,version_number,status,created_by_user_id,published_at,archived_at) values (?,?,3,'ARCHIVED',?,now(),now())",
                archived, f.prescription, f.nutritionistAccount
        );

        summary(f.nutritionistAccount, f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$.current.id").value(f.version.toString()))
                .andExpect(jsonPath("$.current.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.draft.id").value(draft.toString()))
                .andExpect(jsonPath("$.draft.meals.length()").value(0))
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[0].number").value(3))
                .andExpect(jsonPath("$.history[0].status").value("ARCHIVED"))
                .andExpect(jsonPath("$.history[0].archivedAt").exists())
                .andExpect(jsonPath("$.history[2].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.history[0].meals").doesNotExist());
    }

    @Test
    void marksTotalsIncompleteWhenAnItemHasNoPortion() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        var lunch = f.prescribedMeal("LUNCH", macroFood(f, "Rice"));
        var apple = f.food("Apple");
        jdbc.update(
                "insert into prescribed_meal_item(id,prescribed_meal_id,food_id,quantity,unit,item_order) values (?,?,?,1,'unit',1)",
                UUID.randomUUID(), lunch.id(), apple.foodId()
        );
        f.publish();

        summary(f.nutritionistAccount, f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$.current.meals[0].items[1].foodName").value("Apple"))
                .andExpect(jsonPath("$.current.meals[0].items[1].portionLabel").doesNotExist())
                .andExpect(jsonPath("$.current.meals[0].items[1].nutrients.energyKcal").doesNotExist())
                .andExpect(jsonPath("$.current.meals[0].items[1].nutrients.complete").value(false))
                .andExpect(jsonPath("$.current.meals[0].totals.energyKcal").value(100.0))
                .andExpect(jsonPath("$.current.meals[0].totals.complete").value(false))
                .andExpect(jsonPath("$.current.totals.complete").value(false));
    }

    @Test
    void substitutionsAreListedButStayOutOfTheTotals() throws Exception {
        var f = new TrackingFixture(jdbc, "SUBSTITUTIONS_ONLY");
        f.link();
        var lunch = f.prescribedMeal("LUNCH", macroFood(f, "Rice"));
        f.substitution(lunch.items().getFirst(), macroFood(f, "Quinoa"));
        f.publish();

        summary(f.nutritionistAccount, f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$.current.meals[0].items[0].substitutions[0].foodName").value("Quinoa"))
                .andExpect(jsonPath("$.current.meals[0].items[0].substitutions[0].portionLabel").value("Quinoa 100 g"))
                .andExpect(jsonPath("$.current.totals.energyKcal").value(100.0));
    }

    @Test
    void rejectsANutritionistWithoutAnActiveRelationship() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");

        summary(f.nutritionistAccount, f.patient).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PRESCRIPTION_ACCESS_DENIED"));
    }

    @Test
    void anotherNutritionistDoesNotSeeThePrescription() throws Exception {
        var f = new TrackingFixture(jdbc, "RESTRICTED");
        f.link();
        f.prescribedMeal("LUNCH", f.food("Rice"));
        var other = new TrackingFixture(jdbc, "RESTRICTED");
        jdbc.update(
                "update nutritionist_patient_relationship set status='ENDED', ended_at=now(), ended_by_user_account_id=? where patient_id=?",
                f.nutritionistAccount, f.patient
        );
        jdbc.update(
                "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id) values (?,?,?)",
                UUID.randomUUID(), other.nutritionist, f.patient
        );

        summary(other.nutritionistAccount, f.patient).andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionId").doesNotExist())
                .andExpect(jsonPath("$.history.length()").value(0));
    }
}

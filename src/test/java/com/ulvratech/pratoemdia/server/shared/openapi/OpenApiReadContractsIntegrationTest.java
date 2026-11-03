package com.ulvratech.pratoemdia.server.shared.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contracts of the read routes the portal and the app generate their types from. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class OpenApiReadContractsIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Test
    void documentsThePrescriptionSummarySchema() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/v1/nutritionists/me/patients/{patientId}/prescription-summary'].get")
                                .exists()
                )
                .andExpect(jsonPath("$.components.schemas.PrescriptionSummary.properties.current").exists())
                .andExpect(jsonPath("$.components.schemas.PrescriptionSummary.properties.draft").exists())
                .andExpect(jsonPath("$.components.schemas.PrescriptionSummary.properties.history").exists())
                .andExpect(jsonPath("$.components.schemas.Macros.properties.complete").exists())
                .andExpect(jsonPath("$.components.schemas.ItemSummary.properties.foodName").exists());
    }

    @Test
    void documentsTheDailyMealCategories() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/daily-meal-categories'].get").exists())
                .andExpect(jsonPath("$.components.schemas.MealCategory.properties.blockedReason").exists());
    }
}

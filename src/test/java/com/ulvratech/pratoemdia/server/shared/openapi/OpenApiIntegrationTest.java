package com.ulvratech.pratoemdia.server.shared.openapi;

import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
class OpenApiIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Test
    void publishesTheControllerBasedSpecificationWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Prato em Dia API"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/foods'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/prescriptions/{prescriptionId}'].get").exists());
    }

    @Test
    void documentsDiaryAndExecutionContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/calendar-periods'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/daily-nutrition-summary'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/meal-executions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/meal-executions/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/meal-executions/{id}/move'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/patients/me/meal-executions/{id}/restore'].post").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/planned-meals/{plannedMealId}/completion-events']").doesNotExist()
                );
    }

    @Test
    void describesBearerAuthenticationOnlyOnProtectedOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/me'].get.security[0].bearerAuth").isArray());
    }

    @Test
    void exposesSwaggerWithoutWeakeningApiAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresInvalidBearerTokensOnPublicDocumentationEndpoints() throws Exception {
        mockMvc.perform(
                get("/v3/api-docs")
                        .header("Authorization", "Bearer invalid-token")
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                get("/swagger-ui/index.html")
                        .header("Authorization", "Bearer invalid-token")
        )
                .andExpect(status().isOk());
    }

    @Test
    void groupsOperationsWithCustomNamesAndDescribesEveryEndpoint() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/foods'].get.tags[0]").value("Alimentos"))
                .andExpect(
                        jsonPath("$.paths['/api/v1/auth/login'].post.tags[0]")
                                .value("Autenticação")
                )
                .andReturn()
                .getResponse()
                .getContentAsString();
        var document = Json.mapper().readTree(response);
        var httpMethods = Set.of("get", "post", "put", "patch", "delete");
        var paths = document.path("paths");
        var pathNames = paths.fieldNames();

        while (pathNames.hasNext()) {
            var pathName = pathNames.next();
            var path = paths.path(pathName);
            var methodNames = path.fieldNames();
            while (methodNames.hasNext()) {
                var methodName = methodNames.next();
                if (httpMethods.contains(methodName)) {
                    var operation = path.path(methodName);
                    assertFalse(
                            operation.path("summary").asText().isBlank(),
                            methodName + " " + pathName + " must have a summary"
                    );
                    assertFalse(
                            operation.path("description").asText().isBlank(),
                            methodName + " " + pathName + " must have a description"
                    );
                }
            }
        }
    }
}

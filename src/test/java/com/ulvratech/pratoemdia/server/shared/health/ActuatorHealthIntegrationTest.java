package com.ulvratech.pratoemdia.server.shared.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "management.server.port=0"
})
class ActuatorHealthIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    int applicationPort;

    @Value("${local.management.port}")
    int managementPort;

    private HttpResponse<String> get(int port, String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    @Test
    void exposesLivenessAndReadinessOnTheManagementPortWithoutAuthentication() throws Exception {
        var liveness = get(managementPort, "/actuator/health/liveness");
        var readiness = get(managementPort, "/actuator/health/readiness");

        assertEquals(200, liveness.statusCode());
        assertTrue(liveness.body().contains("\"UP\""));
        assertEquals(200, readiness.statusCode());
        assertTrue(readiness.body().contains("\"UP\""));
    }

    @Test
    void doesNotServeActuatorOnTheApplicationPort() throws Exception {
        var response = get(applicationPort, "/actuator/health/readiness");

        assertTrue(response.statusCode() == 401 || response.statusCode() == 404);
    }

    @Test
    void exposesPrometheusMetricsOnTheManagementPort() throws Exception {
        var response = get(managementPort, "/actuator/prometheus");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("jvm_memory_used_bytes"));
    }

    @Test
    void doesNotLeakOtherActuatorEndpoints() throws Exception {
        var status = get(managementPort, "/actuator/env").statusCode();
        assertTrue(status == 401 || status == 404);
    }
}

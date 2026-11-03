package com.ulvratech.pratoemdia.server.features.consent.presentation;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "consent.enforced=true",
        "consent.current-versions.TERMS_OF_USE=v2",
        "consent.current-versions.PRIVACY_POLICY=v1",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class ConsentIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordHasher hasher;

    private Cookie loginAs(UserAccountRole role) throws Exception {
        var email = "u" + UUID.randomUUID() + "@example.com";
        accounts.save(new UserAccount(email, hasher.hash("password-long"), role, UserAccountStatus.ACTIVE));
        return mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-long\"}")
        ).andReturn().getResponse().getCookie("access_token");
    }

    private String accept(String type, String version) {
        return "{\"documentType\":\"" + type + "\",\"version\":\"" + version + "\"}";
    }

    @Test
    void patientIsBlockedUntilBothCurrentVersionsAreAccepted() throws Exception {
        var access = loginAs(UserAccountRole.PATIENT);

        mockMvc.perform(get("/api/v1/foods").cookie(access))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
        mockMvc.perform(get("/api/v1/me/consents").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upToDate").value(false))
                .andExpect(jsonPath("$.documents.length()").value(2));

        mockMvc.perform(
                post("/api/v1/me/consents").cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content(accept("TERMS_OF_USE", "v2"))
        ).andExpect(status().isOk()).andExpect(jsonPath("$.upToDate").value(false));
        mockMvc.perform(get("/api/v1/foods").cookie(access)).andExpect(status().isForbidden());

        mockMvc.perform(
                post("/api/v1/me/consents").cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content(accept("PRIVACY_POLICY", "v1"))
        ).andExpect(status().isOk()).andExpect(jsonPath("$.upToDate").value(true));
        mockMvc.perform(get("/api/v1/foods").cookie(access)).andExpect(status().isOk());
    }

    @Test
    void acceptingAnOutdatedVersionOrUnknownDocumentIsRejected() throws Exception {
        var access = loginAs(UserAccountRole.NUTRITIONIST);

        mockMvc.perform(
                post("/api/v1/me/consents").cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content(accept("TERMS_OF_USE", "v1"))
        ).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONSENT_VERSION_MISMATCH"));
        mockMvc.perform(
                post("/api/v1/me/consents").cookie(access).contentType(MediaType.APPLICATION_JSON)
                        .content(accept("COOKIES", "v1"))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void adminIsNotAskedToAccept() throws Exception {
        var access = loginAs(UserAccountRole.ADMIN);

        mockMvc.perform(get("/api/v1/foods").cookie(access)).andExpect(status().isOk());
    }
}

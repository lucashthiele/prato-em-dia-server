package com.ulvratech.pratoemdia.server.features.audit.presentation;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
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
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class AuditEventsIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordHasher hasher;
    @Autowired
    RecordAuditEvent record;

    private UserAccount create(UserAccountRole role) {
        return accounts.save(
                new UserAccount(
                        "u" + UUID.randomUUID() + "@example.com", hasher.hash("password-long"), role,
                        UserAccountStatus.ACTIVE
                )
        );
    }

    private Cookie login(UserAccount account) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + account.getEmail() + "\",\"password\":\"password-long\"}")
        ).andReturn().getResponse().getCookie("access_token");
    }

    @Test
    void adminListsEventsNewestFirstWithFilters() throws Exception {
        var admin = create(UserAccountRole.ADMIN);
        var target = create(UserAccountRole.NUTRITIONIST);
        record.execute(
                new RecordAuditEventInput(admin.getId(), AuditAction.NUTRITIONIST_CREATED, target.getId(), "first")
        );
        record.execute(
                new RecordAuditEventInput(admin.getId(), AuditAction.USER_STATUS_CHANGED, target.getId(), "second")
        );

        var access = login(admin);

        mockMvc.perform(get("/api/v1/admin/audit-events").param("targetId", target.getId().toString()).cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].details").value("second"))
                .andExpect(jsonPath("$.content[0].action").value("USER_STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[0].actorAccountId").value(admin.getId().toString()))
                .andExpect(jsonPath("$.content[1].details").value("first"));
        mockMvc.perform(
                get("/api/v1/admin/audit-events").param("targetId", target.getId().toString())
                        .param("action", "NUTRITIONIST_CREATED").cookie(access)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void invalidFiltersAreRejected() throws Exception {
        var access = login(create(UserAccountRole.ADMIN));

        mockMvc.perform(get("/api/v1/admin/audit-events").param("action", "NOPE").cookie(access))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                get("/api/v1/admin/audit-events").param("from", "2026-11-03T12:00:00Z")
                        .param("to", "2026-11-03T11:00:00Z").cookie(access)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUDIT_INVALID_FILTER"));
    }

    @Test
    void onlyAdminsCanRead() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-events")).andExpect(status().isUnauthorized());
        for (var role : new UserAccountRole[] { UserAccountRole.PATIENT, UserAccountRole.NUTRITIONIST }) {
            mockMvc.perform(get("/api/v1/admin/audit-events").cookie(login(create(role))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }
}

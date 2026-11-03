package com.ulvratech.pratoemdia.server.features.admin.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.DispatchDueEmails;
import com.ulvratech.pratoemdia.server.features.email.application.port.out.EmailSender;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AdminIntegrationTest {
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
    DispatchDueEmails dispatch;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper json;
    @MockitoBean
    EmailSender sender;

    private UserAccount admin() {
        return accounts.save(
                new UserAccount(
                        "admin" + UUID.randomUUID() + "@example.com", hasher.hash("password-long"),
                        UserAccountRole.ADMIN, UserAccountStatus.ACTIVE
                )
        );
    }

    private Cookie login(String email, String password) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
        ).andExpect(status().isOk()).andReturn().getResponse().getCookie("access_token");
    }

    private JsonNode createNutritionist(Cookie adminAccess, String email) throws Exception {
        var body = mockMvc.perform(
                post("/api/v1/admin/nutritionists").cookie(adminAccess).contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"email\":\"" + email
                                        + "\",\"fullName\":\"Dra. Ana Souza\",\"registrationNumber\":\"CRN-3 12345\"}"
                        )
        )
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private long auditCount(UUID target, String action) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM audit_event WHERE target_account_id = ? AND action = ?", Long.class, target,
                action
        );
    }

    @Test
    void fullAccessJourneyOfANewNutritionist() throws Exception {
        var adminAccount = admin();
        var adminAccess = login(adminAccount.getEmail(), "password-long");
        var email = "ana" + UUID.randomUUID() + "@example.com";

        var created = createNutritionist(adminAccess, email);
        var id = UUID.fromString(created.get("id").asText());
        assertFalse(created.toString().toLowerCase().contains("password"));
        assertFalse(created.toString().toLowerCase().contains("senha"));
        assertEquals("PENDING", created.get("accessEmailStatus").asText());
        assertEquals(1, auditCount(id, "NUTRITIONIST_CREATED"));

        mockMvc.perform(get("/api/v1/admin/users/" + id).cookie(adminAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("NUTRITIONIST"))
                .andExpect(jsonPath("$.fullName").value("Dra. Ana Souza"))
                .andExpect(jsonPath("$.registrationNumber").value("CRN-3 12345"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.accessEmail.status").value("PENDING"));

        dispatch.execute();

        var sent = ArgumentCaptor.forClass(OutgoingEmail.class);
        verify(sender, atLeastOnce()).send(sent.capture());
        var message = sent.getAllValues().stream().filter(m -> m.to().equals(email)).findFirst().orElseThrow();
        var temporary = message.body().lines().filter(l -> l.startsWith("Senha temporária: ")).findFirst().orElseThrow()
                .substring("Senha temporária: ".length());
        mockMvc.perform(get("/api/v1/admin/users/" + id).cookie(adminAccess))
                .andExpect(jsonPath("$.accessEmail.status").value("SENT"));

        var nutritionistAccess = login(email, temporary);
        mockMvc.perform(get("/api/v1/foods").cookie(nutritionistAccess))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mockMvc.perform(post("/api/v1/admin/nutritionists/" + id + "/access-email").cookie(adminAccess))
                .andExpect(status().isAccepted());
        assertEquals(1, auditCount(id, "ACCESS_EMAIL_RESENT"));

        mockMvc.perform(
                patch("/api/v1/me/password").cookie(nutritionistAccess).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"a-brand-new-password\"}")
        ).andExpect(status().isOk());

        dispatch.execute();
        mockMvc.perform(get("/api/v1/admin/users/" + id).cookie(adminAccess))
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.accessEmail.status").value("FAILED"))
                .andExpect(jsonPath("$.accessEmail.lastError").value("PASSWORD_ALREADY_CHANGED"));
        mockMvc.perform(post("/api/v1/admin/nutritionists/" + id + "/access-email").cookie(adminAccess))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCESS_EMAIL_NOT_AVAILABLE"));
        login(email, "a-brand-new-password");
    }

    @Test
    void duplicateEmailAndInvalidBodyAreRejected() throws Exception {
        var adminAccess = login(admin().getEmail(), "password-long");
        var email = "dup" + UUID.randomUUID() + "@example.com";
        createNutritionist(adminAccess, email);

        mockMvc.perform(
                post("/api/v1/admin/nutritionists").cookie(adminAccess).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"fullName\":\"Outra\"}")
        ).andExpect(status().isConflict());
        mockMvc.perform(
                post("/api/v1/admin/nutritionists").cookie(adminAccess).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"fullName\":\"\"}")
        ).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listsUsersWithFiltersAndPagination() throws Exception {
        var adminAccess = login(admin().getEmail(), "password-long");
        var marker = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            createNutritionist(adminAccess, "list" + i + "-" + marker + "@example.com");
        }

        mockMvc.perform(
                get("/api/v1/admin/users").param("q", marker).param("role", "NUTRITIONIST").param("size", "2")
                        .cookie(adminAccess)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].accessEmail.status").value("PENDING"));
        mockMvc.perform(get("/api/v1/admin/users").param("q", marker).param("role", "PATIENT").cookie(adminAccess))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/users").param("q", "%").cookie(adminAccess))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/users").param("role", "ROBOT").cookie(adminAccess))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivatingAnAccountBlocksLoginAndRefreshAndIsAudited() throws Exception {
        var adminAccount = admin();
        var adminAccess = login(adminAccount.getEmail(), "password-long");
        var other = accounts.save(
                new UserAccount(
                        "target" + UUID.randomUUID() + "@example.com", hasher.hash("password-long"),
                        UserAccountRole.PATIENT, UserAccountStatus.ACTIVE
                )
        );
        var session = mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + other.getEmail() + "\",\"password\":\"password-long\"}")
        ).andReturn().getResponse();

        mockMvc.perform(
                patch("/api/v1/admin/users/" + other.getId() + "/status").cookie(adminAccess)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}")
        ).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(session.getCookie("refresh_token")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + other.getEmail() + "\",\"password\":\"password-long\"}")
        ).andExpect(status().isUnauthorized());
        assertEquals(1, auditCount(other.getId(), "USER_STATUS_CHANGED"));
        mockMvc.perform(get("/api/v1/admin/users/" + other.getId()).cookie(adminAccess))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(
                patch("/api/v1/admin/users/" + adminAccount.getId() + "/status").cookie(adminAccess)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}")
        ).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ADMIN_CANNOT_CHANGE_OWN_STATUS"));
        mockMvc.perform(
                patch("/api/v1/admin/users/" + UUID.randomUUID() + "/status").cookie(adminAccess)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}")
        ).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_ACCOUNT_NOT_FOUND"));
    }

    @Test
    void onlyAdminsReachTheAdminRoutesAndTheOldCreateAccountRouteIsGone() throws Exception {
        var patient = accounts.save(
                new UserAccount(
                        "pat" + UUID.randomUUID() + "@example.com", hasher.hash("password-long"),
                        UserAccountRole.PATIENT, UserAccountStatus.ACTIVE
                )
        );
        var patientAccess = login(patient.getEmail(), "password-long");

        mockMvc.perform(get("/api/v1/admin/users").cookie(patientAccess)).andExpect(status().isForbidden());
        mockMvc.perform(
                post("/api/v1/admin/nutritionists").cookie(patientAccess).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"fullName\":\"X\"}")
        ).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());

        var adminAccess = login(admin().getEmail(), "password-long");
        mockMvc.perform(
                post("/api/v1/identity/useraccounts").cookie(adminAccess).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"y@example.com\",\"password\":\"password-long\",\"role\":\"ADMIN\"}")
        ).andExpect(status().isNotFound());
    }
}

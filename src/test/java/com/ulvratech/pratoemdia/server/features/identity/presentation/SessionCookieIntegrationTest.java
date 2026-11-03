package com.ulvratech.pratoemdia.server.features.identity.presentation;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "security.session.allowed-origins=https://portal.example.com",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class SessionCookieIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordHasher hasher;

    private String createUser() {
        var email = "u" + UUID.randomUUID() + "@example.com";
        accounts.save(
                new UserAccount(email, hasher.hash("password-long"), UserAccountRole.PATIENT, UserAccountStatus.ACTIVE)
        );
        return email;
    }

    private MvcResult login(String email) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-long\"}")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andReturn();
    }

    @Test
    void loginSetsHttpOnlyCookiesAndTheAccessCookieAuthenticatesRequests() throws Exception {
        var result = login(createUser());
        var access = result.getResponse().getCookie("access_token");
        var refresh = result.getResponse().getCookie("refresh_token");

        assertNotNull(access);
        assertNotNull(refresh);
        assertEquals(true, access.isHttpOnly());
        assertEquals("/api/v1/auth", refresh.getPath());

        mockMvc.perform(get("/api/v1/foods").cookie(access)).andExpect(status().isOk());
    }

    @Test
    void theBearerHeaderIsNoLongerAccepted() throws Exception {
        var access = login(createUser()).getResponse().getCookie("access_token");

        mockMvc.perform(get("/api/v1/foods").header("Authorization", "Bearer " + access.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTheCookiesAndTheOldRefreshTokenStopsWorking() throws Exception {
        var refresh = login(createUser()).getResponse().getCookie("refresh_token");

        var refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();
        assertNotNull(refreshed.getResponse().getCookie("refresh_token"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshTokenAndClearsTheCookies() throws Exception {
        var result = login(createUser());
        var access = result.getResponse().getCookie("access_token");
        var refresh = result.getResponse().getCookie("refresh_token");

        var logout = mockMvc.perform(post("/api/v1/auth/logout").cookie(access, refresh))
                .andExpect(status().isNoContent())
                .andReturn();
        assertEquals(0, logout.getResponse().getCookie("access_token").getMaxAge());
        assertEquals(0, logout.getResponse().getCookie("refresh_token").getMaxAge());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void writesFromForeignOriginsAreForbiddenButAllowedOriginsAndNativeClientsPass() throws Exception {
        var email = createUser();
        var body = "{\"email\":\"" + email + "\",\"password\":\"password-long\"}";

        mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Origin", "https://evil.example.com")
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGIN_NOT_ALLOWED"));
        mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Origin", "https://portal.example.com")
        ).andExpect(status().isOk());
    }
}

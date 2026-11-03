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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "security.login-rate-limit.max-attempts-per-email=3",
        "security.login-rate-limit.max-attempts-per-ip=4",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class LoginRateLimitIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordHasher hasher;

    private ResultActions login(String email, String password, String ip) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
        );
    }

    @Test
    void anEmailIsBlockedAfterTooManyFailuresEvenWithTheRightPasswordAndFromAnotherIp() throws Exception {
        var email = "limited" + UUID.randomUUID() + "@example.com";
        accounts.save(
                new UserAccount(email, hasher.hash("password-long"), UserAccountRole.ADMIN, UserAccountStatus.ACTIVE)
        );

        for (int i = 0; i < 3; i++) {
            login(email, "wrong", "10.0.0.1").andExpect(status().isUnauthorized());
        }

        login(email, "password-long", "10.0.0.2")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void anIpIsBlockedAcrossEmailsAndOtherIpsKeepWorking() throws Exception {
        var ok = "fine" + UUID.randomUUID() + "@example.com";
        accounts.save(
                new UserAccount(ok, hasher.hash("password-long"), UserAccountRole.ADMIN, UserAccountStatus.ACTIVE)
        );
        for (int i = 0; i < 4; i++) {
            login("ghost" + i + UUID.randomUUID() + "@example.com", "x", "10.0.1.1")
                    .andExpect(status().isUnauthorized());
        }

        login(ok, "password-long", "10.0.1.1").andExpect(status().isTooManyRequests());
        login(ok, "password-long", "10.0.1.2").andExpect(status().isOk());
    }

    @Test
    void successfulLoginsAreNeverCounted() throws Exception {
        var email = "happy" + UUID.randomUUID() + "@example.com";
        accounts.save(
                new UserAccount(email, hasher.hash("password-long"), UserAccountRole.ADMIN, UserAccountStatus.ACTIVE)
        );

        for (int i = 0; i < 6; i++) {
            login(email, "password-long", "10.0.2.1").andExpect(status().isOk());
        }
    }
}

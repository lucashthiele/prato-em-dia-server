package com.ulvratech.pratoemdia.server.features.email.infrastructure;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueuePlanEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;
import com.ulvratech.pratoemdia.server.features.email.application.exception.EmailDeliveryException;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.DispatchDueEmails;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueueAccessEmail;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueuePlanEmail;
import com.ulvratech.pratoemdia.server.features.email.application.port.out.EmailSender;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class EmailOutboxIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    EnqueueAccessEmail enqueueAccess;
    @Autowired
    EnqueuePlanEmail enqueuePlan;
    @Autowired
    DispatchDueEmails dispatch;
    @Autowired
    OutboxEmailRepository outbox;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordHasher hasher;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean
    EmailSender sender;

    @org.junit.jupiter.api.BeforeEach
    void cleanOutbox() {
        jdbc.update("DELETE FROM email_outbox");
    }

    private UserAccount account(boolean passwordChangeRequired) {
        return accounts.save(
                new UserAccount(
                        "n" + UUID.randomUUID() + "@example.com", hasher.hash("original-password"),
                        UserAccountRole.NUTRITIONIST, UserAccountStatus.ACTIVE, passwordChangeRequired
                )
        );
    }

    @Test
    void accessEmailSendsAFreshTemporaryPasswordThatIsStoredHashedOnlyWhenSent() {
        var account = account(true);
        var id = enqueueAccess.execute(new EnqueueAccessEmailInput(account.getId(), account.getEmail(), "Nutri", null));
        assertEquals(null, outbox.findById(id).orElseThrow().getBody());

        dispatch.execute();

        var sent = ArgumentCaptor.forClass(OutgoingEmail.class);
        verify(sender).send(sent.capture());
        var body = sent.getValue().body();
        var password = body.lines().filter(l -> l.startsWith("Senha temporária: ")).findFirst().orElseThrow()
                .substring("Senha temporária: ".length());
        var stored = accounts.findById(account.getId()).orElseThrow();
        assertTrue(hasher.matches(password, stored.getPasswordHash()));
        assertTrue(stored.isPasswordChangeRequired());
        var row = outbox.findById(id).orElseThrow();
        assertEquals(EmailStatus.SENT, row.getStatus());
        assertEquals(null, row.getBody());
    }

    @Test
    void aFailedAccessAttemptKeepsThePreviousPasswordAndSchedulesARetry() {
        var account = account(true);
        var id = enqueueAccess.execute(new EnqueueAccessEmailInput(account.getId(), account.getEmail(), "Nutri", null));
        doThrow(new EmailDeliveryException("HTTP 503", true)).when(sender).send(any());

        dispatch.execute();

        var stored = accounts.findById(account.getId()).orElseThrow();
        assertTrue(hasher.matches("original-password", stored.getPasswordHash()));
        var row = outbox.findById(id).orElseThrow();
        assertEquals(EmailStatus.PENDING, row.getStatus());
        assertEquals(1, row.getAttempts());
        assertTrue(row.getNextAttemptAt().isAfter(Instant.now().plusSeconds(500)));
        assertEquals("HTTP 503", row.getLastError());
    }

    @Test
    void accessEmailIsNotRegeneratedOnceThePasswordWasChanged() {
        var account = account(false);
        var id = enqueueAccess.execute(new EnqueueAccessEmailInput(account.getId(), account.getEmail(), "Nutri", null));

        dispatch.execute();

        verify(sender, never()).send(any());
        assertEquals(EmailStatus.FAILED, outbox.findById(id).orElseThrow().getStatus());
        assertTrue(
                hasher.matches("original-password", accounts.findById(account.getId()).orElseThrow().getPasswordHash())
        );
    }

    @Test
    void planEmailKeepsItsBodyAndIsSentOnce() {
        enqueuePlan
                .execute(new EnqueuePlanEmailInput(EmailType.PLAN_GENERATED, "p@example.com", "Plano", "Corpo único"));

        dispatch.execute();
        dispatch.execute();

        var sent = ArgumentCaptor.forClass(OutgoingEmail.class);
        verify(sender).send(sent.capture());
        assertEquals("Corpo único", sent.getValue().body());
    }

    @Test
    void onlyDueEmailsAreSelected() {
        enqueuePlan.execute(new EnqueuePlanEmailInput(EmailType.PLAN_GENERATED, "x@example.com", "S", "B"));

        assertFalse(outbox.findDueIds(Instant.now().plusSeconds(5), 50).isEmpty());
        assertTrue(outbox.findDueIds(Instant.parse("2000-01-01T00:00:00Z"), 50).isEmpty());
        assertNotEquals(0, outbox.findDueIds(Instant.now().plusSeconds(5), 1).size());
    }
}

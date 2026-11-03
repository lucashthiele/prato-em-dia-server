package com.ulvratech.pratoemdia.server.features.email.application.usecase;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueuePlanEmailInput;
import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnqueueEmailUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private final OutboxEmailRepository repository = mock(OutboxEmailRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void planEmailIsStoredPendingWithItsBody() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        new EnqueuePlanEmailUseCase(repository, clock)
                .execute(new EnqueuePlanEmailInput(EmailType.PLAN_GENERATED, "a@example.com", "Assunto", "Corpo"));

        var saved = ArgumentCaptor.forClass(OutboxEmail.class);
        verify(repository).save(saved.capture());
        assertEquals("Corpo", saved.getValue().getBody());
        assertEquals(EmailStatus.PENDING, saved.getValue().getStatus());
        assertEquals(NOW, saved.getValue().getNextAttemptAt());
    }

    @Test
    void accessEmailIsStoredAsAReferenceOnly() {
        var account = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        new EnqueueAccessEmailUseCase(repository, clock)
                .execute(new EnqueueAccessEmailInput(account, "n@example.com", "Nutri", "Admin"));

        var saved = ArgumentCaptor.forClass(OutboxEmail.class);
        verify(repository).save(saved.capture());
        assertEquals(EmailType.ACCESS, saved.getValue().getType());
        assertEquals(account, saved.getValue().getAccountId());
        assertNull(saved.getValue().getBody());
    }
}

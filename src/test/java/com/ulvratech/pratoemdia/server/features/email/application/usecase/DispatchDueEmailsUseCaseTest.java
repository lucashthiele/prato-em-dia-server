package com.ulvratech.pratoemdia.server.features.email.application.usecase;

import com.ulvratech.pratoemdia.server.features.email.application.exception.EmailDeliveryException;
import com.ulvratech.pratoemdia.server.features.email.application.port.out.EmailSender;
import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InitialPasswordNotAvailableException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RotateInitialPassword;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchDueEmailsUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private final OutboxEmailRepository repository = mock(OutboxEmailRepository.class);
    private final EmailSender sender = mock(EmailSender.class);
    private final RotateInitialPassword rotate = mock(RotateInitialPassword.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final DispatchDueEmailsUseCase useCase = new DispatchDueEmailsUseCase(
            repository, sender, rotate, new TransactionTemplate(transactionManager),
            Clock.fixed(NOW, ZoneOffset.UTC), "https://app.example.com"
    );

    private OutboxEmail plan() {
        var email = OutboxEmail.plan(EmailType.PLAN_GENERATED, "maria@example.com", "Assunto", "Corpo", NOW);
        when(repository.findByIdForUpdate(email.getId())).thenReturn(Optional.of(email));
        when(repository.findDueIds(NOW, DispatchDueEmailsUseCase.BATCH_SIZE)).thenReturn(List.of(email.getId()));
        return email;
    }

    private OutboxEmail access() {
        var email = OutboxEmail.access(UUID.randomUUID(), "nutri@example.com", "Dra. Ana", "Administração", NOW);
        when(repository.findByIdForUpdate(email.getId())).thenReturn(Optional.of(email));
        when(repository.findDueIds(NOW, DispatchDueEmailsUseCase.BATCH_SIZE)).thenReturn(List.of(email.getId()));
        return email;
    }

    @Test
    void sendsThePlanEmailWithItsStoredBodyAndMarksItSent() {
        var email = plan();

        useCase.execute();

        var sent = ArgumentCaptor.forClass(OutgoingEmail.class);
        verify(sender).send(sent.capture());
        assertEquals("maria@example.com", sent.getValue().to());
        assertEquals("Assunto", sent.getValue().subject());
        assertEquals("Corpo", sent.getValue().body());
        assertEquals(EmailStatus.SENT, email.getStatus());
        verify(repository).save(email);
        verify(rotate, never()).execute(any());
    }

    @Test
    void serverFailureReschedulesTheEmailTenMinutesLater() {
        var email = plan();
        doThrow(new EmailDeliveryException("503", true)).when(sender).send(any());

        useCase.execute();

        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(1, email.getAttempts());
        assertEquals(NOW.plusSeconds(600), email.getNextAttemptAt());
        verify(repository).save(email);
    }

    @Test
    void thirdServerFailureKillsTheEmail() {
        var email = plan();
        doThrow(new EmailDeliveryException("503", true)).when(sender).send(any());

        useCase.execute();
        useCase.execute();
        useCase.execute();

        assertEquals(EmailStatus.DEAD, email.getStatus());
        assertEquals(3, email.getAttempts());
    }

    @Test
    void providerRejectionIsPermanent() {
        var email = plan();
        doThrow(new EmailDeliveryException("422 invalid", false)).when(sender).send(any());

        useCase.execute();

        assertEquals(EmailStatus.FAILED, email.getStatus());
        assertEquals("422 invalid", email.getLastError());
    }

    @Test
    void accessEmailGetsAFreshPasswordAtSendTimeAndSaysItReplacesEarlierOnes() {
        var email = access();
        when(rotate.execute(email.getAccountId())).thenReturn("Temp-Pass-123");

        useCase.execute();

        var sent = ArgumentCaptor.forClass(OutgoingEmail.class);
        verify(sender).send(sent.capture());
        assertEquals("nutri@example.com", sent.getValue().to());
        assertTrue(sent.getValue().body().contains("Temp-Pass-123"));
        assertTrue(sent.getValue().body().contains("substitui"));
        assertTrue(sent.getValue().body().contains("https://app.example.com"));
        assertEquals(EmailStatus.SENT, email.getStatus());
        assertEquals(null, email.getBody());
    }

    @Test
    void whenAnAccessEmailFailsThePasswordChangeIsRolledBackAndOnlyTheAttemptIsRecorded() {
        var email = access();
        when(rotate.execute(email.getAccountId())).thenReturn("Temp-Pass-123");
        doThrow(new EmailDeliveryException("timeout", true)).when(sender).send(any());

        useCase.execute();

        verify(transactionManager, times(1)).rollback(any());
        verify(transactionManager, times(1)).commit(any());
        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(1, email.getAttempts());
    }

    @Test
    void accessEmailIsDroppedOnceThePasswordWasAlreadyChanged() {
        var email = access();
        when(rotate.execute(email.getAccountId())).thenThrow(new InitialPasswordNotAvailableException());

        useCase.execute();

        verify(sender, never()).send(any());
        assertEquals(EmailStatus.FAILED, email.getStatus());
        assertEquals("PASSWORD_ALREADY_CHANGED", email.getLastError());
    }

    @Test
    void anUnexpectedErrorOnOneEmailDoesNotStopTheOthers() {
        var broken = UUID.randomUUID();
        var good = OutboxEmail.plan(EmailType.PLAN_GENERATED, "a@example.com", "S", "B", NOW);
        when(repository.findDueIds(NOW, DispatchDueEmailsUseCase.BATCH_SIZE)).thenReturn(List.of(broken, good.getId()));
        when(repository.findByIdForUpdate(broken)).thenThrow(new IllegalStateException("db"));
        when(repository.findByIdForUpdate(good.getId())).thenReturn(Optional.of(good));

        useCase.execute();

        assertEquals(EmailStatus.SENT, good.getStatus());
    }

    @Test
    void emailsAlreadyFinishedByAnotherInstanceAreSkipped() {
        var email = plan();
        email.markSent(NOW);

        useCase.execute();

        verify(sender, never()).send(any());
    }
}

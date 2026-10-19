package com.ulvratech.pratoemdia.server.features.planning.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter.UserAccountQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter.PatientContact;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.NotificationEmailSender;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class EmailPlanGeneratedNotifierTest {
    private final ProfileQueryAdapter profiles = mock(ProfileQueryAdapter.class);
    private final UserAccountQueryAdapter accounts = mock(UserAccountQueryAdapter.class);
    private final NotificationEmailSender sender = mock(NotificationEmailSender.class);
    private final EmailPlanGeneratedNotifier notifier = new EmailPlanGeneratedNotifier(profiles, accounts, sender);
    private final UUID patient = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void sendsImmediatelyWithoutTransaction() {
        stubContact();

        notifier.planGenerated(notice(true));

        var body = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("maria@example.com"), eq("Seu plano semanal está pronto"), body.capture());
        assertTrue(body.getValue().contains("Olá, Maria."));
    }

    @Test
    void sendsOnlyAfterCommitInsideTransaction() {
        stubContact();
        TransactionSynchronizationManager.initSynchronization();

        notifier.planGenerated(notice(false));

        verify(sender, never()).send(any(), any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(sender).send(eq("maria@example.com"), any(), any());
    }

    @Test
    void doesNotSendWhenTransactionNeverCommits() {
        stubContact();
        TransactionSynchronizationManager.initSynchronization();

        notifier.planGenerated(notice(false));

        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void lookupFailureIsSwallowed() {
        when(profiles.patientContact(patient)).thenReturn(Optional.empty());

        notifier.planGenerated(notice(true));

        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void senderFailureIsSwallowed() {
        stubContact();
        when(sender.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException()));

        notifier.planGenerated(notice(true));

        verify(sender).send(any(), any(), any());
    }

    @Test
    void bodyDescribesAutomaticPlan() {
        var body = PlanGeneratedEmailBody.build("Maria", notice(true));

        assertTrue(body.contains("Geramos seu plano semanal automaticamente."));
        assertTrue(body.contains("Período: 20/10 a 26/10"));
        assertTrue(body.contains("Refeições no plano: 14"));
        assertTrue(body.contains("Refeições por dia: 2"));
        assertTrue(body.contains("Tipos de refeição: Café da manhã, Jantar"));
        assertTrue(body.contains("até 20/10 às 00:00"));
        assertFalse(body.contains("http"));
    }

    @Test
    void bodyDescribesManualRegeneration() {
        var body = PlanGeneratedEmailBody.build("Maria", notice(false));

        assertTrue(body.contains("regenerado a seu pedido"));
        assertEquals(1, body.split("Olá, Maria\\.", -1).length - 1);
    }

    private void stubContact() {
        when(profiles.patientContact(patient)).thenReturn(Optional.of(new PatientContact("Maria", account)));
        when(accounts.email(account)).thenReturn("maria@example.com");
        when(sender.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private PlanGeneratedNotice notice(boolean automatic) {
        return new PlanGeneratedNotice(
                patient,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 26),
                Instant.parse("2026-10-20T03:00:00Z"),
                automatic,
                14,
                List.of(MealType.BREAKFAST, MealType.DINNER)
        );
    }
}

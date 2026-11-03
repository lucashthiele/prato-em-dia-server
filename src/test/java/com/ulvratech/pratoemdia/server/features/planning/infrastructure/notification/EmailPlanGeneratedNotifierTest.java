package com.ulvratech.pratoemdia.server.features.planning.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter.UserAccountQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter.PatientContact;
import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueuePlanEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueuePlanEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailPlanGeneratedNotifierTest {
    private final ProfileQueryAdapter profiles = mock(ProfileQueryAdapter.class);
    private final UserAccountQueryAdapter accounts = mock(UserAccountQueryAdapter.class);
    private final EnqueuePlanEmail outbox = mock(EnqueuePlanEmail.class);
    private final EmailPlanGeneratedNotifier notifier = new EmailPlanGeneratedNotifier(profiles, accounts, outbox);
    private final UUID patient = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();

    @Test
    void enqueuesThePlanEmailWithItsBodyInTheCallersTransaction() {
        stubContact();

        notifier.planGenerated(notice(true));

        var input = ArgumentCaptor.forClass(EnqueuePlanEmailInput.class);
        verify(outbox).execute(input.capture());
        assertEquals(EmailType.PLAN_GENERATED, input.getValue().type());
        assertEquals("maria@example.com", input.getValue().recipient());
        assertEquals("Seu plano semanal está pronto", input.getValue().subject());
        assertTrue(input.getValue().body().contains("Olá, Maria."));
    }

    @Test
    void lookupFailureIsSwallowed() {
        when(profiles.patientContact(patient)).thenReturn(Optional.empty());

        notifier.planGenerated(notice(true));

        verify(outbox, never()).execute(any());
    }

    @Test
    void rejectedGenerationEmailsThePatientToContactTheNutritionist() {
        stubContact();

        notifier.planGenerationRejected(patient);

        var input = ArgumentCaptor.forClass(EnqueuePlanEmailInput.class);
        verify(outbox).execute(input.capture());
        assertEquals(EmailType.PLAN_GENERATION_REJECTED, input.getValue().type());
        assertEquals("maria@example.com", input.getValue().recipient());
        assertTrue(input.getValue().body().contains("Olá, Maria."));
        assertTrue(input.getValue().body().contains("nutricionista"));
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

package com.ulvratech.pratoemdia.server.features.planning.infrastructure.notification;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter.UserAccountQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanGeneratedNotifier;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.NotificationEmailSender;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
public class EmailPlanGeneratedNotifier implements PlanGeneratedNotifier {
    private final ProfileQueryAdapter profiles;
    private final UserAccountQueryAdapter accounts;
    private final NotificationEmailSender sender;

    public EmailPlanGeneratedNotifier(
            ProfileQueryAdapter profiles, UserAccountQueryAdapter accounts, NotificationEmailSender sender
    ) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.sender = sender;
    }

    @Override
    public void planGenerated(PlanGeneratedNotice notice) {
        String email;
        String body;
        try {
            var contact = profiles.patientContact(notice.patientId())
                    .orElseThrow(() -> new IllegalStateException("Patient profile not found"));
            email = accounts.email(contact.userAccountId());
            body = PlanGeneratedEmailBody.build(contact.fullName(), notice);
        } catch (RuntimeException error) {
            log.error("Failed to prepare plan generated email. patientId={}", notice.patientId(), error);
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send(notice, email, body);
                        }
                    }
            );
        else
            send(notice, email, body);
    }

    private void send(PlanGeneratedNotice notice, String email, String body) {
        try {
            sender.send(email, PlanGeneratedEmailBody.SUBJECT, body).whenComplete(
                    (ignored, error) -> {
                        if (error != null)
                            log.error("Failed to send plan generated email. patientId={}", notice.patientId(), error);
                    }
            );
        } catch (RuntimeException error) {
            log.error("Failed to schedule plan generated email. patientId={}", notice.patientId(), error);
        }
    }
}

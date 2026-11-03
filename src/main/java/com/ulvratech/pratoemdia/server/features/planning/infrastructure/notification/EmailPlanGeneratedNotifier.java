package com.ulvratech.pratoemdia.server.features.planning.infrastructure.notification;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueuePlanEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueuePlanEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter.UserAccountQueryAdapter;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanGeneratedNotifier;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter.ProfileQueryAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stores plan emails in the email outbox (LUC-66), which retries server-side failures. The outbox
 * row joins the caller's transaction when there is one.
 */
@Component
@Slf4j
public class EmailPlanGeneratedNotifier implements PlanGeneratedNotifier {
    private final ProfileQueryAdapter profiles;
    private final UserAccountQueryAdapter accounts;
    private final EnqueuePlanEmail outbox;

    public EmailPlanGeneratedNotifier(
            ProfileQueryAdapter profiles, UserAccountQueryAdapter accounts, EnqueuePlanEmail outbox
    ) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.outbox = outbox;
    }

    @Override
    public void planGenerated(PlanGeneratedNotice notice) {
        enqueue(
                notice.patientId(), EmailType.PLAN_GENERATED, PlanGeneratedEmailBody.SUBJECT,
                PlanGeneratedEmailBody::build, notice
        );
    }

    @Override
    public void planGenerationRejected(UUID patientId) {
        enqueue(
                patientId, EmailType.PLAN_GENERATION_REJECTED, PlanGenerationRejectedEmailBody.SUBJECT,
                (name, ignored) -> PlanGenerationRejectedEmailBody.build(name), null
        );
    }

    private <T> void enqueue(
            UUID patientId, EmailType type, String subject, java.util.function.BiFunction<String, T, String> body,
            T context
    ) {
        try {
            var contact = profiles.patientContact(patientId)
                    .orElseThrow(() -> new IllegalStateException("Patient profile not found"));
            outbox.execute(
                    new EnqueuePlanEmailInput(
                            type, accounts.email(contact.userAccountId()), subject,
                            body.apply(contact.fullName(), context)
                    )
            );
        } catch (RuntimeException error) {
            log.error("Failed to enqueue {} email. patientId={}", type, patientId, error);
        }
    }
}

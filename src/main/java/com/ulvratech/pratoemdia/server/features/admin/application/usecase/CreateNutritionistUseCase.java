package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.CreateNutritionistInput;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.CreateNutritionistOutput;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.CreateNutritionist;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueueAccessEmail;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionNutritionistAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account, profile, access-email reference and audit event are written in one transaction. */
@Service
@Transactional
public class CreateNutritionistUseCase implements CreateNutritionist {
    private final ProvisionNutritionistAccount provision;
    private final EnqueueAccessEmail enqueueAccessEmail;
    private final RecordAuditEvent audit;

    public CreateNutritionistUseCase(
            ProvisionNutritionistAccount provision, EnqueueAccessEmail enqueueAccessEmail, RecordAuditEvent audit
    ) {
        this.provision = provision;
        this.enqueueAccessEmail = enqueueAccessEmail;
        this.audit = audit;
    }

    @Override
    public CreateNutritionistOutput execute(CreateNutritionistInput input) {
        var account = provision.execute(
                new ProvisionNutritionistAccountInput(input.email(), input.fullName(), input.registrationNumber())
        );
        var fullName = input.fullName().strip();
        enqueueAccessEmail
                .execute(new EnqueueAccessEmailInput(account.userAccountId(), account.email(), fullName, null));
        audit.execute(
                new RecordAuditEventInput(
                        input.actorAccountId(), AuditAction.NUTRITIONIST_CREATED, account.userAccountId(),
                        "email=" + account.email()
                )
        );
        var registration = input.registrationNumber() == null || input.registrationNumber().isBlank()
                ? null
                : input.registrationNumber().strip();
        return new CreateNutritionistOutput(
                account.userAccountId(), account.email(), fullName, registration, "PENDING"
        );
    }
}

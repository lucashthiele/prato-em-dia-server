package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.exception.AccessEmailNotAvailableException;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ResendNutritionistAccess;
import com.ulvratech.pratoemdia.server.features.admin.application.port.out.AdminUserQuery;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueueAccessEmail;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountNotFoundException;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ResendNutritionistAccessUseCase implements ResendNutritionistAccess {
    private final AdminUserQuery query;
    private final EnqueueAccessEmail enqueueAccessEmail;
    private final RecordAuditEvent audit;

    public ResendNutritionistAccessUseCase(
            AdminUserQuery query, EnqueueAccessEmail enqueueAccessEmail, RecordAuditEvent audit
    ) {
        this.query = query;
        this.enqueueAccessEmail = enqueueAccessEmail;
        this.audit = audit;
    }

    @Override
    public void execute(UUID actorAccountId, UUID nutritionistAccountId) {
        var user = query.findById(nutritionistAccountId).orElseThrow(UserAccountNotFoundException::new);
        if (user.role() != UserAccountRole.NUTRITIONIST
                || user.status() != UserAccountStatus.ACTIVE
                || !user.passwordChangeRequired()) {
            throw new AccessEmailNotAvailableException();
        }
        enqueueAccessEmail.execute(
                new EnqueueAccessEmailInput(user.id(), user.email(), user.fullName(), null)
        );
        audit.execute(
                new RecordAuditEventInput(actorAccountId, AuditAction.ACCESS_EMAIL_RESENT, user.id(), null)
        );
    }
}

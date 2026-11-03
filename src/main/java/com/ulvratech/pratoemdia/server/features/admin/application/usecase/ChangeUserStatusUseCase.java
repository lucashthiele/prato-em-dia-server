package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.ChangeUserStatusInput;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.AdminCannotChangeOwnStatusException;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ChangeUserStatus;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangeUserAccountStatusInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangeUserAccountStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeUserStatusUseCase implements ChangeUserStatus {
    private final ChangeUserAccountStatus changeStatus;
    private final RecordAuditEvent audit;

    public ChangeUserStatusUseCase(ChangeUserAccountStatus changeStatus, RecordAuditEvent audit) {
        this.changeStatus = changeStatus;
        this.audit = audit;
    }

    @Override
    public void execute(ChangeUserStatusInput input) {
        if (input.actorAccountId().equals(input.targetAccountId())) {
            throw new AdminCannotChangeOwnStatusException();
        }
        var previous = changeStatus.execute(
                new ChangeUserAccountStatusInput(input.targetAccountId(), input.status())
        );
        if (previous != input.status()) {
            audit.execute(
                    new RecordAuditEventInput(
                            input.actorAccountId(), AuditAction.USER_STATUS_CHANGED, input.targetAccountId(),
                            previous + " -> " + input.status()
                    )
            );
        }
    }
}

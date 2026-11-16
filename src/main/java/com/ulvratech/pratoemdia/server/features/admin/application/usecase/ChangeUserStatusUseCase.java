package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.ChangeUserStatusInput;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.AdminCannotChangeOwnStatusException;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.EndRelationshipsNotApplicableException;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ChangeUserStatus;
import com.ulvratech.pratoemdia.server.features.admin.application.port.out.AdminUserQuery;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangeUserAccountStatusInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangeUserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountNotFoundException;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.EndAllNutritionistRelationships;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeUserStatusUseCase implements ChangeUserStatus {
    private final ChangeUserAccountStatus changeStatus;
    private final RecordAuditEvent audit;
    private final AdminUserQuery query;
    private final EndAllNutritionistRelationships endRelationships;

    public ChangeUserStatusUseCase(
            ChangeUserAccountStatus changeStatus, RecordAuditEvent audit, AdminUserQuery query,
            EndAllNutritionistRelationships endRelationships
    ) {
        this.changeStatus = changeStatus;
        this.audit = audit;
        this.query = query;
        this.endRelationships = endRelationships;
    }

    @Override
    public void execute(ChangeUserStatusInput input) {
        if (input.actorAccountId().equals(input.targetAccountId())) {
            throw new AdminCannotChangeOwnStatusException();
        }
        if (input.endRelationships()) {
            var target = query.findById(input.targetAccountId()).orElseThrow(UserAccountNotFoundException::new);
            if (target.role() != UserAccountRole.NUTRITIONIST || input.status() != UserAccountStatus.INACTIVE)
                throw new EndRelationshipsNotApplicableException();
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
        if (input.endRelationships()) {
            int ended = endRelationships.execute(input.actorAccountId(), input.targetAccountId());
            audit.execute(
                    new RecordAuditEventInput(
                            input.actorAccountId(), AuditAction.RELATIONSHIPS_ENDED, input.targetAccountId(),
                            "count=" + ended
                    )
            );
        }
    }
}

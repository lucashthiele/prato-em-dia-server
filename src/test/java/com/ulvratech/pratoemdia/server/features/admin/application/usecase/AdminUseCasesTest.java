package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.ChangeUserStatusInput;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.CreateNutritionistInput;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.AccessEmailNotAvailableException;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.AdminCannotChangeOwnStatusException;
import com.ulvratech.pratoemdia.server.features.admin.application.port.out.AdminUserQuery;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueueAccessEmail;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangeUserAccountStatusInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountNotFoundException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangeUserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionNutritionistAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUseCasesTest {
    private final ProvisionNutritionistAccount provision = mock(ProvisionNutritionistAccount.class);
    private final EnqueueAccessEmail enqueueAccess = mock(EnqueueAccessEmail.class);
    private final RecordAuditEvent audit = mock(RecordAuditEvent.class);
    private final ChangeUserAccountStatus changeStatus = mock(ChangeUserAccountStatus.class);
    private final AdminUserQuery query = mock(AdminUserQuery.class);
    private final UUID admin = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    private AdminUserView view(UserAccountRole role, boolean passwordChangeRequired, UserAccountStatus status) {
        return new AdminUserView(
                target, "n@example.com", role, status, passwordChangeRequired, "Nutri", "CRN-3 1",
                Instant.parse("2026-11-01T00:00:00Z"), null
        );
    }

    @Test
    void createsTheNutritionistQueuesTheAccessEmailAndAuditsInThatOrder() {
        when(provision.execute(any())).thenReturn(new ProvisionNutritionistAccountOutput(target, "n@example.com"));

        var output = new CreateNutritionistUseCase(provision, enqueueAccess, audit)
                .execute(new CreateNutritionistInput(admin, "N@Example.com", "Nutri", "CRN-3 1"));

        var order = inOrder(provision, enqueueAccess, audit);
        order.verify(provision).execute(new ProvisionNutritionistAccountInput("N@Example.com", "Nutri", "CRN-3 1"));
        order.verify(enqueueAccess).execute(new EnqueueAccessEmailInput(target, "n@example.com", "Nutri", null));
        order.verify(audit).execute(
                new RecordAuditEventInput(admin, AuditAction.NUTRITIONIST_CREATED, target, "email=n@example.com")
        );
        assertEquals(target, output.userAccountId());
        assertEquals("PENDING", output.accessEmailStatus());
    }

    @Test
    void resendQueuesANewAccessEmailForANutritionistThatHasNotLoggedInYet() {
        when(query.findById(target))
                .thenReturn(Optional.of(view(UserAccountRole.NUTRITIONIST, true, UserAccountStatus.ACTIVE)));

        new ResendNutritionistAccessUseCase(query, enqueueAccess, audit).execute(admin, target);

        verify(enqueueAccess).execute(new EnqueueAccessEmailInput(target, "n@example.com", "Nutri", null));
        verify(audit).execute(new RecordAuditEventInput(admin, AuditAction.ACCESS_EMAIL_RESENT, target, null));
    }

    @Test
    void resendIsRefusedWhenThePasswordWasAlreadyChangedOrTheAccountIsNotANutritionist() {
        var useCase = new ResendNutritionistAccessUseCase(query, enqueueAccess, audit);
        when(query.findById(target))
                .thenReturn(Optional.of(view(UserAccountRole.NUTRITIONIST, false, UserAccountStatus.ACTIVE)));
        assertThrows(AccessEmailNotAvailableException.class, () -> useCase.execute(admin, target));

        when(query.findById(target))
                .thenReturn(Optional.of(view(UserAccountRole.PATIENT, true, UserAccountStatus.ACTIVE)));
        assertThrows(AccessEmailNotAvailableException.class, () -> useCase.execute(admin, target));

        when(query.findById(target))
                .thenReturn(Optional.of(view(UserAccountRole.NUTRITIONIST, true, UserAccountStatus.INACTIVE)));
        assertThrows(AccessEmailNotAvailableException.class, () -> useCase.execute(admin, target));
        verify(enqueueAccess, never()).execute(any());
        verify(audit, never()).execute(any());
    }

    @Test
    void resendOfAnUnknownAccountIsNotFound() {
        when(query.findById(target)).thenReturn(Optional.empty());

        assertThrows(
                UserAccountNotFoundException.class,
                () -> new ResendNutritionistAccessUseCase(query, enqueueAccess, audit).execute(admin, target)
        );
    }

    @Test
    void statusChangeIsAuditedWithPreviousAndNewValue() {
        when(changeStatus.execute(new ChangeUserAccountStatusInput(target, UserAccountStatus.INACTIVE)))
                .thenReturn(UserAccountStatus.ACTIVE);

        new ChangeUserStatusUseCase(changeStatus, audit)
                .execute(new ChangeUserStatusInput(admin, target, UserAccountStatus.INACTIVE));

        verify(audit).execute(
                new RecordAuditEventInput(admin, AuditAction.USER_STATUS_CHANGED, target, "ACTIVE -> INACTIVE")
        );
    }

    @Test
    void anUnchangedStatusIsNotAudited() {
        when(changeStatus.execute(any())).thenReturn(UserAccountStatus.ACTIVE);

        new ChangeUserStatusUseCase(changeStatus, audit)
                .execute(new ChangeUserStatusInput(admin, target, UserAccountStatus.ACTIVE));

        verify(audit, never()).execute(any());
    }

    @Test
    void anAdminCannotChangeTheirOwnStatus() {
        assertThrows(
                AdminCannotChangeOwnStatusException.class,
                () -> new ChangeUserStatusUseCase(changeStatus, audit)
                        .execute(new ChangeUserStatusInput(admin, admin, UserAccountStatus.INACTIVE))
        );
        verify(changeStatus, never()).execute(any());
    }
}

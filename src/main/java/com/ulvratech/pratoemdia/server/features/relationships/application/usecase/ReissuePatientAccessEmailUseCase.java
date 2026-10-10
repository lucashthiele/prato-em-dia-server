package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RotateInitialPassword;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.PatientAccessEmail;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ReissuePatientAccessEmail;
import com.ulvratech.pratoemdia.server.features.relationships.application.service.RelationshipEmailDelivery;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Service
public class ReissuePatientAccessEmailUseCase implements ReissuePatientAccessEmail {
    private final RelationshipRepository repository;
    private final RotateInitialPassword rotate;
    private final RelationshipEmailDelivery emails;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public ReissuePatientAccessEmailUseCase(
            RelationshipRepository r,
            RotateInitialPassword p,
            RelationshipEmailDelivery e,
            TransactionTemplate t,
            Clock c
    ) {
        repository = r;
        rotate = p;
        emails = e;
        transactions = t;
        clock = c;
    }

    public void execute(RelationshipQueryInput i) {
        var access = transactions.execute(
                s -> {
                    var view = repository
                            .findActiveViewByNutritionistAccountAndPatient(
                                    i.accountId(), i.patientId()
                            )
                            .orElseThrow(RelationshipNotFoundException::new);
                    var password = rotate.execute(view.patientUserAccountId());
                    var relationship = repository.findById(view.relationshipId())
                            .orElseThrow(RelationshipNotFoundException::new);
                    int attempt = relationship.startEmailAttempt(clock.instant());
                    repository.save(relationship);
                    return new PatientAccessEmail(view, password, attempt);
                }
        );
        emails.sendInitialAccess(
                access.view().relationshipId(), access.emailAttempt(), access.view().patientFullName(),
                access.view().patientEmail(), access.view().nutritionistFullName(), access.password()
        );
    }
}

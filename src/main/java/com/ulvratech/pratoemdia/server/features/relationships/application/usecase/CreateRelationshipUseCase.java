package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionPatientAccount;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ProvisionedRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.CreateRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.service.RelationshipEmailDelivery;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Service
public class CreateRelationshipUseCase implements CreateRelationship {
    private final RelationshipRepository relationships;
    private final ProvisionPatientAccount accounts;
    private final RelationshipEmailDelivery emails;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public CreateRelationshipUseCase(
            RelationshipRepository relationships,
            ProvisionPatientAccount accounts,
            RelationshipEmailDelivery emails,
            TransactionTemplate transactions,
            Clock clock
    ) {
        this.relationships = relationships;
        this.accounts = accounts;
        this.emails = emails;
        this.transactions = transactions;
        this.clock = clock;
    }

    public RelationshipOutput execute(CreateRelationshipInput input) {
        ProvisionedRelationship provisioned;
        try {
            provisioned = transactions.execute(
                    status -> {
                        var nutritionistId = relationships
                                .findNutritionistProfileIdByAccount(
                                        input.nutritionistAccountId()
                                )
                                .orElseThrow(RelationshipNotFoundException::new);
                        var account = accounts.execute(
                                new ProvisionPatientAccountInput(
                                        input.email(), input.fullName()
                                )
                        );
                        var patientId = relationships
                                .findPatientProfileIdByAccount(
                                        account.userAccountId()
                                )
                                .orElseThrow(RelationshipNotFoundException::new);
                        if (relationships.findActiveByPatient(patientId).isPresent())
                            throw new RelationshipConflictException();
                        var saved = relationships.save(
                                ProfessionalPatientRelationship.create(
                                        nutritionistId, patientId
                                )
                        );
                        int emailAttempt = saved.startEmailAttempt(clock.instant());
                        relationships.save(saved);
                        var view = relationships
                                .findViewById(saved.getId())
                                .orElseThrow(RelationshipNotFoundException::new);
                        return new ProvisionedRelationship(
                                view, account.created(), account.initialPassword(), emailAttempt
                        );
                    }
            );
        } catch (DataIntegrityViolationException e) {
            throw new RelationshipConflictException();
        }
        if (provisioned.created())
            emails.sendInitialAccess(
                    provisioned.view().relationshipId(), provisioned.emailAttempt(),
                    provisioned.view().patientFullName(), provisioned.view().patientEmail(),
                    provisioned.view().nutritionistFullName(), provisioned.initialPassword()
            );
        else
            emails.sendRelationshipNotice(
                    provisioned.view().relationshipId(), provisioned.emailAttempt(),
                    provisioned.view().patientFullName(), provisioned.view().patientEmail(),
                    provisioned.view().nutritionistFullName()
            );
        return RelationshipOutput.from(provisioned.view());
    }
}

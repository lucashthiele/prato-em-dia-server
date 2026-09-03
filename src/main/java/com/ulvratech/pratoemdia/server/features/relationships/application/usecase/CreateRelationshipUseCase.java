package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionPatientAccount;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.CreateRelationshipInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ProvisionedRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.CreateRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Service
@Slf4j
public class CreateRelationshipUseCase implements CreateRelationship {
    private final RelationshipRepository relationships;
    private final ProvisionPatientAccount accounts;
    private final PatientRelationshipEmailSender emails;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public CreateRelationshipUseCase(
        RelationshipRepository relationships,
        ProvisionPatientAccount accounts,
        PatientRelationshipEmailSender emails,
        TransactionTemplate transactions,
        Clock clock) {
        this.relationships = relationships;
        this.accounts = accounts;
        this.emails = emails;
        this.transactions = transactions;
        this.clock = clock;
    }

    public RelationshipOutput execute(CreateRelationshipInput input) {
        ProvisionedRelationship provisioned;
        try {
            provisioned =
                transactions.execute(
                    status -> {
                        var nutritionistId =
                            relationships
                                .findNutritionistProfileIdByAccount(
                                    input.nutritionistAccountId())
                                .orElseThrow(RelationshipNotFoundException::new);
                        var account =
                            accounts.execute(
                                new ProvisionPatientAccountInput(
                                    input.email(), input.fullName()));
                        var patientId =
                            relationships
                                .findPatientProfileIdByAccount(
                                    account.userAccountId())
                                .orElseThrow(RelationshipNotFoundException::new);
                        if (relationships.findActiveByPatient(patientId).isPresent())
                            throw new RelationshipConflictException();
                        var saved =
                            relationships.save(
                                ProfessionalPatientRelationship.create(
                                    nutritionistId, patientId));
                        var view =
                            relationships
                                .findViewById(saved.getId())
                                .orElseThrow(RelationshipNotFoundException::new);
                        return new ProvisionedRelationship(
                            view, account.created(), account.initialPassword());
                    });
        } catch (DataIntegrityViolationException e) {
            throw new RelationshipConflictException();
        }
        var delivery = RelationshipEmailStatus.SENT;
        try {
            if (provisioned.created())
                emails.sendInitialAccess(
                    provisioned.view().patientFullName(),
                    provisioned.view().patientEmail(),
                    provisioned.view().nutritionistFullName(),
                    provisioned.initialPassword());
            else
                emails.sendRelationshipNotice(
                    provisioned.view().patientFullName(),
                    provisioned.view().patientEmail(),
                    provisioned.view().nutritionistFullName());
        } catch (RuntimeException e) {
            delivery = RelationshipEmailStatus.FAILED;
            log.error(
                "Failed to send patient relationship email. operation=create, relationshipId={}",
                provisioned.view().relationshipId(),
                e);
        }
        var result = delivery;
        return transactions.execute(
            status -> {
                var relationship =
                    relationships
                        .findById(provisioned.view().relationshipId())
                        .orElseThrow(RelationshipNotFoundException::new);
                relationship.recordEmail(result, clock.instant());
                relationships.save(relationship);
                return RelationshipOutput.from(
                    relationships
                        .findViewById(relationship.getId())
                        .orElseThrow(RelationshipNotFoundException::new));
            });
    }
}

package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RotateInitialPassword;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.PatientAccessEmail;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipEmailException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ResendPatientAccessEmail;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class ResendPatientAccessEmailUseCase implements ResendPatientAccessEmail {
  private final RelationshipRepository repository;
  private final RotateInitialPassword rotate;
  private final PatientRelationshipEmailSender emails;
  private final TransactionTemplate transactions;
  private final Clock clock;

  public ResendPatientAccessEmailUseCase(
      RelationshipRepository r,
      RotateInitialPassword p,
      PatientRelationshipEmailSender e,
      TransactionTemplate t,
      Clock c) {
    repository = r;
    rotate = p;
    emails = e;
    transactions = t;
    clock = c;
  }

  public void execute(RelationshipQueryInput i) {
    var access =
        transactions.execute(
            s -> {
              var view =
                  repository
                      .findActiveViewByNutritionistAccountAndPatient(i.accountId(), i.patientId())
                      .orElseThrow(RelationshipNotFoundException::new);
              return new PatientAccessEmail(view, rotate.execute(view.patientUserAccountId()));
            });
    boolean sent = true;
    try {
      emails.sendInitialAccess(
          access.view().patientFullName(),
          access.view().patientEmail(),
          access.view().nutritionistFullName(),
          access.password());
    } catch (RuntimeException e) {
      sent = false;
      log.error(
          "Failed to send patient relationship email. operation=resend, relationshipId={}",
          access.view().relationshipId(),
          e);
    }
    var result = sent ? RelationshipEmailStatus.SENT : RelationshipEmailStatus.FAILED;
    transactions.executeWithoutResult(
        s -> {
          var relationship =
              repository
                  .findById(access.view().relationshipId())
                  .orElseThrow(RelationshipNotFoundException::new);
          relationship.recordEmail(result, clock.instant());
          repository.save(relationship);
        });
    if (!sent) throw new RelationshipEmailException();
  }
}

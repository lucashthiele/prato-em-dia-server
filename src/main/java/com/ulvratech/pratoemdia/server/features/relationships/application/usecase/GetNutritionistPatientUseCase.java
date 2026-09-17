package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipQueryInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.GetNutritionistPatient;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import org.springframework.stereotype.Service;

@Service
public class GetNutritionistPatientUseCase implements GetNutritionistPatient {
  private final RelationshipRepository repository;

  public GetNutritionistPatientUseCase(RelationshipRepository r) {
    repository = r;
  }

  public RelationshipOutput execute(RelationshipQueryInput i) {
    return repository
        .findActiveViewByNutritionistAccountAndPatient(i.accountId(), i.patientId())
        .map(RelationshipOutput::from)
        .orElseThrow(RelationshipNotFoundException::new);
  }
}

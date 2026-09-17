package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.application.dto.RelationshipOutput;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.in.ListNutritionistPatients;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.stereotype.Service;

@Service
public class ListNutritionistPatientsUseCase implements ListNutritionistPatients {
  private final RelationshipRepository repository;

  public ListNutritionistPatientsUseCase(RelationshipRepository repository) {
    this.repository = repository;
  }

  public PageResult<RelationshipOutput> execute(ListRelationshipsInput i) {
    var p =
        repository.findActiveByNutritionistAccount(
            i.accountId(), new PageRequest(i.page(), i.size()));
    return new PageResult<>(
        p.content().stream().map(RelationshipOutput::from).toList(),
        p.page(),
        p.size(),
        p.totalElements(),
        p.totalPages());
  }
}

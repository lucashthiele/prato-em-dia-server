package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.mapper.RelationshipMapper;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RelationshipRepositoryAdapter implements RelationshipRepository {
    private final RelationshipJpaRepository repository;
    private final RelationshipMapper mapper;

    public RelationshipRepositoryAdapter(
        RelationshipJpaRepository repository, RelationshipMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ProfessionalPatientRelationship save(ProfessionalPatientRelationship r) {
        return mapper.toDomain(repository.save(mapper.toModel(r)));
    }

    public Optional<ProfessionalPatientRelationship> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    public Optional<ProfessionalPatientRelationship> findActiveByNutritionistAndPatient(
        UUID n, UUID p) {
        return repository
            .findByNutritionistIdAndPatientIdAndStatus(n, p, RelationshipStatus.ACTIVE)
            .map(mapper::toDomain);
    }

    public Optional<ProfessionalPatientRelationship> findActiveByPatient(UUID p) {
        return repository
            .findByPatientIdAndStatus(p, RelationshipStatus.ACTIVE)
            .map(mapper::toDomain);
    }

    public Optional<RelationshipView> findActiveViewByNutritionistAccountAndPatient(
        UUID a, UUID p) {
        return repository.findActiveViewByNutritionist(a, p);
    }

    public Optional<RelationshipView> findActiveViewByPatientAccount(UUID a) {
        return repository.findActiveViewByPatientAccount(a);
    }

    public Optional<RelationshipView> findViewById(UUID id) {
        return repository.findViewById(id);
    }

    public PageResult<RelationshipView> findActiveByNutritionistAccount(UUID a, PageRequest p) {
        var page =
            repository.findActiveByNutritionistAccount(
                a, org.springframework.data.domain.PageRequest.of(p.page(), p.size()));
        return new PageResult<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }

    public Optional<UUID> findNutritionistProfileIdByAccount(UUID a) {
        return repository.findNutritionistProfileIdByAccount(a);
    }

    public Optional<UUID> findPatientProfileIdByAccount(UUID a) {
        return repository.findPatientProfileIdByAccount(a);
    }
}

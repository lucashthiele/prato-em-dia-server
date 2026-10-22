package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.ProfessionalPatientRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipFilter;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipSort;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.mapper.RelationshipMapper;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RelationshipRepositoryAdapter implements RelationshipRepository {
    private final RelationshipJpaRepository repository;
    private final RelationshipMapper mapper;

    public RelationshipRepositoryAdapter(
            RelationshipJpaRepository repository, RelationshipMapper mapper
    ) {
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
            UUID n, UUID p
    ) {
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
            UUID a, UUID p
    ) {
        return repository.findActiveViewByNutritionist(a, p);
    }

    public Optional<RelationshipView> findActiveViewByPatientAccount(UUID a) {
        return repository.findActiveViewByPatientAccount(a);
    }

    public Optional<RelationshipView> findViewById(UUID id) {
        return repository.findViewById(id);
    }

    public PageResult<RelationshipView> findActiveByNutritionistAccount(
            UUID a, RelationshipFilter f, PageRequest p
    ) {
        var pattern = f.search() == null ? "%" : "%" + escapeLike(f.search()) + "%";
        var page = repository.findActiveByNutritionistAccount(
                a,
                pattern,
                f.emailDeliveryStatus() != null,
                f.emailDeliveryStatus() == null ? RelationshipEmailStatus.PENDING
                        : f.emailDeliveryStatus(),
                f.openDraftOnly(),
                org.springframework.data.domain.PageRequest.of(p.page(), p.size(), sort(f.sort()))
        );
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Sort sort(RelationshipSort s) {
        return switch (s) {
            case CREATED_AT_DESC -> JpaSort.unsafe(Sort.Direction.DESC, "r.createdAt", "r.id");
            case CREATED_AT_ASC -> JpaSort.unsafe(Sort.Direction.ASC, "r.createdAt", "r.id");
            case NAME_ASC -> JpaSort.unsafe(Sort.Direction.ASC, "lower(p.fullName)", "r.id");
        };
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public Optional<UUID> findNutritionistProfileIdByAccount(UUID a) {
        return repository.findNutritionistProfileIdByAccount(a);
    }

    public Optional<UUID> findPatientProfileIdByAccount(UUID a) {
        return repository.findPatientProfileIdByAccount(a);
    }
}

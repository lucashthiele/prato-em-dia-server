package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEventFilter;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditEventRepository;
import com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.mapper.AuditEventMapper;
import com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.model.AuditEventModel;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class AuditEventRepositoryAdapter implements AuditEventRepository {
    private final AuditEventJpaRepository repository;
    private final AuditEventMapper mapper;

    public AuditEventRepositoryAdapter(AuditEventJpaRepository repository, AuditEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        return mapper.toEntity(repository.save(mapper.toModel(event)));
    }

    @Override
    public PageResult<AuditEvent> search(AuditEventFilter filter, PageRequest page) {
        Specification<AuditEventModel> spec = (root, query, cb) -> cb.conjunction();
        if (filter.action() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), filter.action()));
        }
        if (filter.actorAccountId() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("actorAccountId"), filter.actorAccountId()));
        }
        if (filter.targetAccountId() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("targetAccountId"), filter.targetAccountId()));
        }
        if (filter.from() != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("occurredAt"), filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("occurredAt"), filter.to()));
        }
        var result = repository.findAll(
                spec,
                org.springframework.data.domain.PageRequest.of(
                        page.page(), page.size(), Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))
                )
        );
        return new PageResult<>(
                result.getContent().stream().map(mapper::toEntity).toList(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
    }
}

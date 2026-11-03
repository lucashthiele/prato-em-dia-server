package com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.mapper.OutboxEmailMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OutboxEmailRepositoryAdapter implements OutboxEmailRepository {
    private final OutboxEmailJpaRepository repository;
    private final OutboxEmailMapper mapper;

    public OutboxEmailRepositoryAdapter(OutboxEmailJpaRepository repository, OutboxEmailMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OutboxEmail save(OutboxEmail email) {
        return mapper.toEntity(repository.save(mapper.toModel(email)));
    }

    @Override
    public Optional<OutboxEmail> findById(UUID id) {
        return repository.findById(id).map(mapper::toEntity);
    }

    @Override
    public Optional<OutboxEmail> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toEntity);
    }

    @Override
    public List<UUID> findDueIds(Instant now, int limit) {
        return repository.findDueIds(now, PageRequest.of(0, limit));
    }

    @Override
    public List<OutboxEmail> findLatestAccessByAccounts(List<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        var latest = new java.util.LinkedHashMap<UUID, OutboxEmail>();
        repository.findAccessByAccounts(accountIds)
                .forEach(m -> latest.putIfAbsent(m.getAccountId(), mapper.toEntity(m)));
        return List.copyOf(latest.values());
    }
}

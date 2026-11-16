package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditAccountIdentity;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditAccountDirectory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class AuditAccountDirectoryAdapter implements AuditAccountDirectory {
    private final AuditAccountJpaRepository repository;

    public AuditAccountDirectoryAdapter(AuditAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<UUID, AuditAccountIdentity> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.findAllById(ids).stream()
                .map(m -> new AuditAccountIdentity(m.getId(), m.getFullName(), m.getEmail()))
                .collect(Collectors.toMap(AuditAccountIdentity::id, Function.identity()));
    }
}

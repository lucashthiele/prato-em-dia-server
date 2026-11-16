package com.ulvratech.pratoemdia.server.features.audit.application.usecase;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.AuditEventView;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.ListAuditEventsInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.ListAuditEvents;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditAccountIdentity;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEventFilter;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditAccountDirectory;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditEventRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ListAuditEventsUseCase implements ListAuditEvents {
    private final AuditEventRepository repository;
    private final AuditAccountDirectory accounts;

    public ListAuditEventsUseCase(AuditEventRepository repository, AuditAccountDirectory accounts) {
        this.repository = repository;
        this.accounts = accounts;
    }

    @Override
    public PageResult<AuditEventView> execute(ListAuditEventsInput input) {
        if (input.from() != null && input.to() != null && input.to().isBefore(input.from())) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        var page = repository.search(
                new AuditEventFilter(
                        input.action(), input.actorAccountId(), input.targetAccountId(), input.from(), input.to()
                ),
                new PageRequest(input.page(), input.size())
        );
        Set<UUID> ids = page.content().stream()
                .flatMap(e -> Stream.of(e.actorAccountId(), e.targetAccountId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var identities = accounts.findByIds(ids);
        return new PageResult<>(
                page.content().stream()
                        .map(
                                e -> new AuditEventView(
                                        e, lookup(identities, e.actorAccountId()),
                                        lookup(identities, e.targetAccountId())
                                )
                        )
                        .toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages()
        );
    }

    private static AuditAccountIdentity lookup(Map<UUID, AuditAccountIdentity> identities, UUID id) {
        return id == null ? null : identities.get(id);
    }
}

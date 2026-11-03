package com.ulvratech.pratoemdia.server.features.audit.application.usecase;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.ListAuditEventsInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.ListAuditEvents;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEventFilter;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditEventRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListAuditEventsUseCase implements ListAuditEvents {
    private final AuditEventRepository repository;

    public ListAuditEventsUseCase(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<AuditEvent> execute(ListAuditEventsInput input) {
        if (input.from() != null && input.to() != null && input.to().isBefore(input.from())) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        return repository.search(
                new AuditEventFilter(
                        input.action(), input.actorAccountId(), input.targetAccountId(), input.from(), input.to()
                ),
                new PageRequest(input.page(), input.size())
        );
    }
}

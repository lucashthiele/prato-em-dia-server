package com.ulvratech.pratoemdia.server.features.audit.application.usecase;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.RecordAuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class RecordAuditEventUseCase implements RecordAuditEvent {
    private final AuditEventRepository repository;
    private final Clock clock;

    public RecordAuditEventUseCase(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void execute(RecordAuditEventInput input) {
        repository.save(
                new AuditEvent(
                        UUID.randomUUID(), clock.instant(), input.actorAccountId(), input.action(),
                        input.targetAccountId(), input.details()
                )
        );
    }
}

package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.model.AuditEventModel;
import org.springframework.stereotype.Component;

@Component
public class AuditEventMapper {
    public AuditEvent toEntity(AuditEventModel m) {
        return new AuditEvent(
                m.getId(), m.getOccurredAt(), m.getActorAccountId(), m.getAction(), m.getTargetAccountId(),
                m.getDetails()
        );
    }

    public AuditEventModel toModel(AuditEvent e) {
        return AuditEventModel.builder()
                .id(e.id())
                .occurredAt(e.occurredAt())
                .actorAccountId(e.actorAccountId())
                .action(e.action())
                .targetAccountId(e.targetAccountId())
                .details(e.details())
                .build();
    }
}

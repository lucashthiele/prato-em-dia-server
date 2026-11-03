package com.ulvratech.pratoemdia.server.features.audit.presentation.web.response;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id, Instant occurredAt, UUID actorAccountId, String action, UUID targetAccountId, String details) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.id(), event.occurredAt(), event.actorAccountId(), event.action().name(),
                event.targetAccountId(), event.details()
        );
    }
}

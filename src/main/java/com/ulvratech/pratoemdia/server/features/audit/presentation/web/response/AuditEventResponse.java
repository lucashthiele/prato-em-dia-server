package com.ulvratech.pratoemdia.server.features.audit.presentation.web.response;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.AuditEventView;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id, Instant occurredAt, UUID actorAccountId, String actorName, String action, UUID targetAccountId,
        String targetName, String targetEmail, String details) {
    public static AuditEventResponse from(AuditEventView view) {
        var event = view.event();
        return new AuditEventResponse(
                event.id(), event.occurredAt(), event.actorAccountId(),
                view.actor() == null ? null : view.actor().name(),
                event.action().name(), event.targetAccountId(), view.target() == null ? null : view.target().name(),
                view.target() == null ? null : view.target().email(), event.details()
        );
    }
}

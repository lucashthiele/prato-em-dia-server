package com.ulvratech.pratoemdia.server.features.audit.domain.entity;

import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id, Instant occurredAt, UUID actorAccountId, AuditAction action, UUID targetAccountId, String details) {
}

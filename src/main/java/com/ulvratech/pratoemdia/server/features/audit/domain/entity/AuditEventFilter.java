package com.ulvratech.pratoemdia.server.features.audit.domain.entity;

import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditEventFilter(
        AuditAction action, UUID actorAccountId, UUID targetAccountId, Instant from, Instant to) {
}

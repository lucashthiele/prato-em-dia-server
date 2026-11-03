package com.ulvratech.pratoemdia.server.features.audit.application.dto;

import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record ListAuditEventsInput(
        AuditAction action, UUID actorAccountId, UUID targetAccountId, Instant from, Instant to, int page, int size) {
}

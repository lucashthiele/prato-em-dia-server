package com.ulvratech.pratoemdia.server.features.audit.application.dto;

import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;

import java.util.UUID;

public record RecordAuditEventInput(UUID actorAccountId, AuditAction action, UUID targetAccountId, String details) {
}

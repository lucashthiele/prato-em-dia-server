package com.ulvratech.pratoemdia.server.features.audit.application.dto;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditAccountIdentity;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;

/** An audit event with the identities of its actor and target; each is null when there is no such account. */
public record AuditEventView(AuditEvent event, AuditAccountIdentity actor, AuditAccountIdentity target) {
}

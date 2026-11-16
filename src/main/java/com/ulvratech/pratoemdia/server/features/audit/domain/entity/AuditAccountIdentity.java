package com.ulvratech.pratoemdia.server.features.audit.domain.entity;

import java.util.UUID;

/** Name and e-mail of an account referenced by an audit event. {@code name} is null for accounts without profile. */
public record AuditAccountIdentity(UUID id, String name, String email) {
}

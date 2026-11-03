package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditEventModel {
    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_account_id", nullable = false)
    private UUID actorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "target_account_id")
    private UUID targetAccountId;

    @Column(length = 500)
    private String details;
}

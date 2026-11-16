package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Read-only mapping of {@code admin_user_view}, used to resolve the names shown in the audit trail. */
@Entity
@Immutable
@Table(name = "admin_user_view")
@Getter
public class AuditAccountModel {
    @Id
    private UUID id;

    private String email;

    @Column(name = "full_name")
    private String fullName;
}

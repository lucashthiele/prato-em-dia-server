package com.ulvratech.pratoemdia.server.features.admin.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** Read-only mapping of the {@code admin_user_view} database view. */
@Entity
@Immutable
@Table(name = "admin_user_view")
@Getter
public class AdminUserModel {
    @Id
    private UUID id;

    private String email;

    @Enumerated(EnumType.STRING)
    private UserAccountRole role;

    @Enumerated(EnumType.STRING)
    private UserAccountStatus status;

    @Column(name = "password_change_required")
    private boolean passwordChangeRequired;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "access_email_status")
    private String accessEmailStatus;

    @Column(name = "access_email_attempts")
    private Integer accessEmailAttempts;

    @Column(name = "access_email_error")
    private String accessEmailError;
}

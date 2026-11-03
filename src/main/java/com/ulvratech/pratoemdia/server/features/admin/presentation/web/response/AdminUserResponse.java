package com.ulvratech.pratoemdia.server.features.admin.presentation.web.response;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String role,
        String status,
        boolean passwordChangeRequired,
        String fullName,
        String registrationNumber,
        Instant createdAt,
        AccessEmailResponse accessEmail) {
    /** {@code status} is PENDING, SENT, FAILED or DEAD (DEAD only for outbox emails). */
    public record AccessEmailResponse(String status, int attempts, String lastError) {
    }

    public static AdminUserResponse from(AdminUserView view) {
        return new AdminUserResponse(
                view.id(), view.email(), view.role().name(), view.status().name(), view.passwordChangeRequired(),
                view.fullName(), view.registrationNumber(), view.createdAt(),
                view.accessEmail() == null
                        ? null
                        : new AccessEmailResponse(
                                view.accessEmail().status(), view.accessEmail().attempts(),
                                view.accessEmail().lastError()
                        )
        );
    }
}

package com.ulvratech.pratoemdia.server.features.admin.application.dto;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

import java.util.UUID;

public record ChangeUserStatusInput(
        UUID actorAccountId, UUID targetAccountId, UserAccountStatus status, boolean endRelationships) {
    public ChangeUserStatusInput(UUID actorAccountId, UUID targetAccountId, UserAccountStatus status) {
        this(actorAccountId, targetAccountId, status, false);
    }
}

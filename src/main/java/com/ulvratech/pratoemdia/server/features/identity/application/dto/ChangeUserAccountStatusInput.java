package com.ulvratech.pratoemdia.server.features.identity.application.dto;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

import java.util.UUID;

public record ChangeUserAccountStatusInput(UUID userAccountId, UserAccountStatus status) {
}

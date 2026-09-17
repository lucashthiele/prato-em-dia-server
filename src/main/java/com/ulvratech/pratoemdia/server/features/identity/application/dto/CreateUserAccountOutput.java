package com.ulvratech.pratoemdia.server.features.identity.application.dto;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import java.util.UUID;

public record CreateUserAccountOutput(
    UUID id, String email, UserAccountRole role, UserAccountStatus status) {}

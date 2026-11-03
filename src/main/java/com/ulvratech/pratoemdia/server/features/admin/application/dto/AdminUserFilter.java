package com.ulvratech.pratoemdia.server.features.admin.application.dto;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;

public record AdminUserFilter(UserAccountRole role, UserAccountStatus status, String query) {
}

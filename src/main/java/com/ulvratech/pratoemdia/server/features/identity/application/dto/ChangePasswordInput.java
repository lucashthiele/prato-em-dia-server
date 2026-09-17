package com.ulvratech.pratoemdia.server.features.identity.application.dto;

import java.util.UUID;

public record ChangePasswordInput(UUID userAccountId, String currentPassword, String newPassword) {}

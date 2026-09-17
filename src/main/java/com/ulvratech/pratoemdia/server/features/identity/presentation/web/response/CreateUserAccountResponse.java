package com.ulvratech.pratoemdia.server.features.identity.presentation.web.response;

import java.util.UUID;

public record CreateUserAccountResponse(UUID userId, String email, String role, String status) {}

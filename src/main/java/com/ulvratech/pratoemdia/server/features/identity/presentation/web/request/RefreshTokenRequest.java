package com.ulvratech.pratoemdia.server.features.identity.presentation.web.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Informe o token de renovação.") String refreshToken) {}

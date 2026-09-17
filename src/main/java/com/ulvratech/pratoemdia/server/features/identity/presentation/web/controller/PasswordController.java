package com.ulvratech.pratoemdia.server.features.identity.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangePasswordInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangePassword;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.ChangePasswordRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.response.AuthenticationResponse;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/password")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@Tag(name = "Senha", description = "Gerenciamento da senha do usuário autenticado.")
public class PasswordController {
    private final ChangePassword changePassword;

    public PasswordController(ChangePassword changePassword) {
        this.changePassword = changePassword;
    }

    @PatchMapping
    @Operation(summary = "Alterar senha", description = "Substitui a senha atual e emite um novo par de tokens de autenticação.")
    AuthenticationResponse change(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request
    ) {
        var output = changePassword.execute(
                new ChangePasswordInput(
                        UUID.fromString(jwt.getSubject()),
                        request.currentPassword(),
                        request.newPassword()
                )
        );
        return new AuthenticationResponse(
                output.accessToken(),
                output.refreshToken(),
                output.tokenType(),
                output.expiresIn(),
                output.passwordChangeRequired()
        );
    }
}

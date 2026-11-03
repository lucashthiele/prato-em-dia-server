package com.ulvratech.pratoemdia.server.features.identity.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangePasswordInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangePassword;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.ChangePasswordRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.response.AuthenticationResponse;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.session.SessionCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/password")
@SecurityRequirement(name = OpenApiConfiguration.COOKIE_AUTH)
@Tag(name = "Senha", description = "Gerenciamento da senha do usuário autenticado.")
public class PasswordController {
    private final ChangePassword changePassword;
    private final SessionCookies sessionCookies;

    public PasswordController(ChangePassword changePassword, SessionCookies sessionCookies) {
        this.changePassword = changePassword;
        this.sessionCookies = sessionCookies;
    }

    @PatchMapping
    @Operation(summary = "Alterar senha", description = "Substitui a senha atual, revoga as sessões anteriores e renova os cookies de sessão.")
    ResponseEntity<AuthenticationResponse> change(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request
    ) {
        var output = changePassword.execute(
                new ChangePasswordInput(
                        UUID.fromString(jwt.getSubject()),
                        request.currentPassword(),
                        request.newPassword()
                )
        );
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        sessionCookies.access(output.accessToken(), output.expiresIn()).toString()
                )
                .header(HttpHeaders.SET_COOKIE, sessionCookies.refresh(output.refreshToken()).toString())
                .body(new AuthenticationResponse(output.expiresIn(), output.passwordChangeRequired()));
    }
}

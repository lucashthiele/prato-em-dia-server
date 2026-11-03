package com.ulvratech.pratoemdia.server.features.identity.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.LogoutInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.AuthenticateUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.LogoutUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RefreshAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.LoginRequest;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.session.SessionCookies;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.response.AuthenticationResponse;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, renovação e encerramento de sessões por cookies HttpOnly.")
public class AuthenticationController {
    private final AuthenticateUser authenticateUser;
    private final RefreshAccessToken refreshAccessToken;
    private final LogoutUser logoutUser;
    private final SessionCookies sessionCookies;

    public AuthenticationController(
            AuthenticateUser authenticateUser,
            RefreshAccessToken refreshAccessToken,
            LogoutUser logoutUser,
            SessionCookies sessionCookies
    ) {
        this.authenticateUser = authenticateUser;
        this.refreshAccessToken = refreshAccessToken;
        this.logoutUser = logoutUser;
        this.sessionCookies = sessionCookies;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida e-mail e senha e inicia a sessão: o access token e o refresh token são entregues em cookies HttpOnly, nunca no corpo.")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        var output = authenticateUser.execute(new AuthenticateUserInput(request.email(), request.password()));
        return session(
                output.accessToken(), output.refreshToken(), output.expiresIn(), output.passwordChangeRequired()
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar sessão", description = "Lê o cookie refresh_token (enviado somente para esta rota) e emite novos cookies de sessão. O refresh token é de uso único.")
    public ResponseEntity<AuthenticationResponse> refresh(
            @CookieValue(name = SessionCookies.REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        var output = refreshAccessToken.execute(new RefreshAccessTokenInput(refreshToken));
        return session(
                output.accessToken(), output.refreshToken(), output.expiresIn(), output.passwordChangeRequired()
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão", description = "Revoga o refresh token do cookie e limpa os cookies de sessão.")
    @SecurityRequirement(name = OpenApiConfiguration.COOKIE_AUTH)
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(name = SessionCookies.REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            logoutUser.execute(new LogoutInput(UUID.fromString(jwt.getSubject()), refreshToken));
        }
        var response = ResponseEntity.noContent();
        sessionCookies.clear().forEach(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));
        return response.build();
    }

    private ResponseEntity<AuthenticationResponse> session(
            String accessToken, String refreshToken, long expiresIn, boolean passwordChangeRequired
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.access(accessToken, expiresIn).toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookies.refresh(refreshToken).toString())
                .body(new AuthenticationResponse(expiresIn, passwordChangeRequired));
    }
}

package com.ulvratech.pratoemdia.server.features.identity.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.LogoutInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.AuthenticateUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.LogoutUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RefreshAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.LoginRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.RefreshTokenRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.response.AuthenticationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticateUser authenticateUser;
    private final RefreshAccessToken refreshAccessToken;
    private final LogoutUser logoutUser;

    public AuthenticationController(
        AuthenticateUser authenticateUser,
        RefreshAccessToken refreshAccessToken,
        LogoutUser logoutUser
    ) {
        this.authenticateUser = authenticateUser;
        this.refreshAccessToken = refreshAccessToken;
        this.logoutUser = logoutUser;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        var output = authenticateUser.execute(
            new AuthenticateUserInput(request.email(), request.password())
        );
        return ResponseEntity.ok(new AuthenticationResponse(
            output.accessToken(),
            output.refreshToken(),
            output.tokenType(),
            output.expiresIn()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        var output = refreshAccessToken.execute(
            new RefreshAccessTokenInput(request.refreshToken())
        );
        return ResponseEntity.ok(new AuthenticationResponse(
            output.accessToken(),
            output.refreshToken(),
            output.tokenType(),
            output.expiresIn()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RefreshTokenRequest request
    ) {
        logoutUser.execute(new LogoutInput(
            UUID.fromString(jwt.getSubject()),
            request.refreshToken()
        ));
        return ResponseEntity.noContent().build();
    }
}

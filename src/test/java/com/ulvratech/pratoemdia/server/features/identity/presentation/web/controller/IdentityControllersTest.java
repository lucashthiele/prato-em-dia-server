package com.ulvratech.pratoemdia.server.features.identity.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangePasswordInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.AuthenticateUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangePassword;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.CreateUserAccount;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.LogoutUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RefreshAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.ChangePasswordRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.CreateUserAccountRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.LoginRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.RefreshTokenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityControllersTest {
    @Test
    void authenticationEndpointsDelegateAndMapResponses() {
        var authenticate = mock(AuthenticateUser.class);
        var refresh = mock(RefreshAccessToken.class);
        var logout = mock(LogoutUser.class);
        when(authenticate.execute(any())).thenReturn(
                new AuthenticateUserOutput("access", "refresh", "Bearer", 900, true)
        );
        when(refresh.execute(any())).thenReturn(
                new RefreshAccessTokenOutput("access2", "refresh2", "Bearer", 800, false)
        );
        var controller = new AuthenticationController(authenticate, refresh, logout);

        var login = controller.login(new LoginRequest("user@example.com", "password"));
        var refreshed = controller.refresh(new RefreshTokenRequest("old"));
        var loggedOut = controller.logout(
                jwt(UUID.randomUUID(), "PATIENT"),
                new RefreshTokenRequest("refresh")
        );

        assertEquals("access", login.getBody().accessToken());
        assertTrue(login.getBody().passwordChangeRequired());
        assertEquals("refresh2", refreshed.getBody().refreshToken());
        assertEquals(204, loggedOut.getStatusCode().value());
        verify(authenticate).execute(new AuthenticateUserInput("user@example.com", "password"));
        verify(refresh).execute(new RefreshAccessTokenInput("old"));
        verify(logout).execute(any());
    }

    @Test
    void passwordEndpointMapsNewTokens() {
        var change = mock(ChangePassword.class);
        when(change.execute(any())).thenReturn(
                new AuthenticateUserOutput("a", "r", "Bearer", 10, false)
        );
        var controller = new PasswordController(change);

        var response = controller.change(
                jwt(UUID.randomUUID(), "PATIENT"),
                new ChangePasswordRequest("old password", "new password long")
        );

        assertEquals("a", response.accessToken());
        assertFalse(response.passwordChangeRequired());
        verify(change).execute(any(ChangePasswordInput.class));
    }

    @Test
    void userAccountEndpointMapsCreatedAccount() {
        var create = mock(CreateUserAccount.class);
        var id = UUID.randomUUID();
        when(create.execute(any())).thenReturn(
                new CreateUserAccountOutput(
                        id, "patient@example.com", UserAccountRole.PATIENT, UserAccountStatus.ACTIVE
                )
        );
        var controller = new UserAccountController(create);

        var response = controller.create(
                new CreateUserAccountRequest(
                        "patient@example.com", "password long", "PATIENT", "Patient"
                )
        );

        assertEquals(201, response.getStatusCode().value());
        assertEquals(id, response.getBody().userId());
        assertEquals("PATIENT", response.getBody().role());
        verify(create).execute(any(CreateUserAccountInput.class));
    }

    @Test
    void accountRequestValidatesRoleDependentName() {
        assertTrue(
                new CreateUserAccountRequest("a@b.com", "password long", "ADMIN", null)
                        .isFullNameValidForRole()
        );
        assertFalse(
                new CreateUserAccountRequest("a@b.com", "password long", "PATIENT", " ")
                        .isFullNameValidForRole()
        );
        assertTrue(
                new CreateUserAccountRequest("a@b.com", "password long", "PATIENT", "Name")
                        .isFullNameValidForRole()
        );
    }

    private static Jwt jwt(UUID id, String role) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(id.toString())
                .claim("role", role).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}

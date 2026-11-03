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
import com.ulvratech.pratoemdia.server.features.identity.config.JwtConfigurationProperties;
import com.ulvratech.pratoemdia.server.features.identity.config.SessionCookieProperties;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.session.SessionCookies;
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
    void authenticationEndpointsDelegateAndDeliverTheSessionInCookies() {
        var authenticate = mock(AuthenticateUser.class);
        var refresh = mock(RefreshAccessToken.class);
        var logout = mock(LogoutUser.class);
        when(authenticate.execute(any())).thenReturn(
                new AuthenticateUserOutput("access", "refresh", "Bearer", 900, true)
        );
        when(refresh.execute(any())).thenReturn(
                new RefreshAccessTokenOutput("access2", "refresh2", "Bearer", 800, false)
        );
        var controller = new AuthenticationController(authenticate, refresh, logout, cookies());

        var login = controller.login(new LoginRequest("user@example.com", "password"));
        var refreshed = controller.refresh("old");
        var loggedOut = controller.logout(jwt(UUID.randomUUID(), "PATIENT"), "refresh");

        assertEquals(900, login.getBody().expiresIn());
        assertTrue(login.getBody().passwordChangeRequired());
        assertTrue(setCookies(login).stream().anyMatch(c -> c.startsWith("access_token=access;")));
        assertTrue(setCookies(login).stream().anyMatch(c -> c.startsWith("refresh_token=refresh;")));
        assertTrue(setCookies(refreshed).stream().anyMatch(c -> c.startsWith("refresh_token=refresh2;")));
        assertEquals(204, loggedOut.getStatusCode().value());
        assertEquals(2, setCookies(loggedOut).size());
        assertTrue(setCookies(loggedOut).stream().allMatch(c -> c.contains("Max-Age=0")));
        verify(authenticate).execute(new AuthenticateUserInput("user@example.com", "password"));
        verify(refresh).execute(new RefreshAccessTokenInput("old"));
        verify(logout).execute(any());
    }

    @Test
    void refreshWithoutTheCookieIsRejectedAsAnInvalidRefreshToken() {
        var controller = new AuthenticationController(
                mock(AuthenticateUser.class), mock(RefreshAccessToken.class), mock(LogoutUser.class), cookies()
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException.class,
                () -> controller.refresh(null)
        );
    }

    @Test
    void logoutWithoutTheRefreshCookieStillClearsTheSession() {
        var logout = mock(LogoutUser.class);
        var controller = new AuthenticationController(
                mock(AuthenticateUser.class), mock(RefreshAccessToken.class), logout, cookies()
        );

        var response = controller.logout(jwt(UUID.randomUUID(), "PATIENT"), null);

        assertEquals(204, response.getStatusCode().value());
        assertEquals(2, setCookies(response).size());
        org.mockito.Mockito.verifyNoInteractions(logout);
    }

    @Test
    void passwordEndpointRenewsTheSessionCookies() {
        var change = mock(ChangePassword.class);
        when(change.execute(any())).thenReturn(
                new AuthenticateUserOutput("a", "r", "Bearer", 10, false)
        );
        var controller = new PasswordController(change, cookies());

        var response = controller.change(
                jwt(UUID.randomUUID(), "PATIENT"),
                new ChangePasswordRequest("old password", "new password long")
        );

        assertEquals(10, response.getBody().expiresIn());
        assertFalse(response.getBody().passwordChangeRequired());
        assertTrue(setCookies(response).stream().anyMatch(c -> c.startsWith("access_token=a;")));
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

    private static SessionCookies cookies() {
        return new SessionCookies(
                new SessionCookieProperties(true, java.util.List.of()),
                new JwtConfigurationProperties(
                        "prato-em-dia", java.time.Duration.ofMinutes(15), java.time.Duration.ofDays(30),
                        "01234567890123456789012345678901"
                )
        );
    }

    private static java.util.List<String> setCookies(org.springframework.http.ResponseEntity<?> response) {
        return response.getHeaders().get("Set-Cookie");
    }

    private static Jwt jwt(UUID id, String role) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(id.toString())
                .claim("role", role).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}

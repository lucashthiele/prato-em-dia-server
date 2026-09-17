package com.ulvratech.pratoemdia.server.features.profiles.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.GetProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.UpdateProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotAvailableException;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.GetCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.UpdateCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.presentation.web.request.UpdateCurrentProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentProfileControllerTest {
    @Test
    void getsAndUpdatesTheCurrentProfile() {
        var get = mock(GetCurrentProfile.class);
        var update = mock(UpdateCurrentProfile.class);
        var id = UUID.randomUUID();
        var output = new ProfileOutput(
                UUID.randomUUID(), id, "PATIENT", "Name", "123",
                null, null, LocalDate.of(2000, 1, 1), Instant.now(), Instant.now()
        );
        when(get.execute(any())).thenReturn(output);
        when(update.execute(any())).thenReturn(output);
        var controller = new CurrentProfileController(get, update);
        var jwt = jwt(id, "PATIENT");

        assertEquals("Name", controller.get(jwt).fullName());
        assertEquals(
                "123", controller.update(
                        jwt, new UpdateCurrentProfileRequest(
                                "Name", "123", null, null, output.birthDate()
                        )
                ).phone()
        );
        verify(get).execute(any(GetProfileInput.class));
        verify(update).execute(any(UpdateProfileInput.class));
    }

    @Test
    void rejectsMissingAndUnknownRoles() {
        var controller = new CurrentProfileController(
                mock(GetCurrentProfile.class),
                mock(UpdateCurrentProfile.class)
        );
        assertThrows(ProfileNotAvailableException.class, () -> controller.get(jwt(UUID.randomUUID(), null)));
        assertThrows(ProfileNotAvailableException.class, () -> controller.get(jwt(UUID.randomUUID(), "OTHER")));
    }

    private static Jwt jwt(UUID id, String role) {
        var builder = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (role != null)
            builder.claim("role", role);
        return builder.build();
    }
}

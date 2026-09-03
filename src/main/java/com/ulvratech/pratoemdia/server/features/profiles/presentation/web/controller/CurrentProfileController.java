package com.ulvratech.pratoemdia.server.features.profiles.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.GetProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.UpdateProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotAvailableException;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.GetCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.UpdateCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.presentation.web.request.UpdateCurrentProfileRequest;
import com.ulvratech.pratoemdia.server.features.profiles.presentation.web.response.CurrentProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentProfileController {
    private final GetCurrentProfile getCurrentProfile;
    private final UpdateCurrentProfile updateCurrentProfile;

    public CurrentProfileController(
        GetCurrentProfile getCurrentProfile,
        UpdateCurrentProfile updateCurrentProfile
    ) {
        this.getCurrentProfile = getCurrentProfile;
        this.updateCurrentProfile = updateCurrentProfile;
    }

    @GetMapping
    CurrentProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        var output = getCurrentProfile.execute(new GetProfileInput(
            UUID.fromString(jwt.getSubject()), profileType(jwt)
        ));
        return CurrentProfileResponse.from(output);
    }

    @PutMapping
    CurrentProfileResponse update(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateCurrentProfileRequest request
    ) {
        var output = updateCurrentProfile.execute(new UpdateProfileInput(
            UUID.fromString(jwt.getSubject()),
            profileType(jwt),
            request.fullName(),
            request.phone(),
            request.registrationNumber(),
            request.bio(),
            request.birthDate()
        ));
        return CurrentProfileResponse.from(output);
    }

    private ProfileType profileType(Jwt jwt) {
        try {
            return ProfileType.valueOf(jwt.getClaimAsString("role"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ProfileNotAvailableException();
        }
    }
}

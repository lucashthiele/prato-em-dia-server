package com.ulvratech.pratoemdia.server.features.relationships.presentation.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePatientRelationshipRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 160) String fullName) {
}

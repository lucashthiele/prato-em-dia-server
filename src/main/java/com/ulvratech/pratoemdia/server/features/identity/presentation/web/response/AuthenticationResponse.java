package com.ulvratech.pratoemdia.server.features.identity.presentation.web.response;

/** Session summary. The tokens themselves travel only in HttpOnly cookies (ADR 0001). */
public record AuthenticationResponse(long expiresIn, boolean passwordChangeRequired) {
}

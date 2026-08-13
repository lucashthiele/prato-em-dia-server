package com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto;

public record ApiErrorResponse(
    int status,
    String message
) {}

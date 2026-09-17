package com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto;

import java.util.List;

public record ApiErrorResponse(
    int status, String code, String message, List<ApiFieldError> errors) {
  public ApiErrorResponse(int status, String code, String message) {
    this(status, code, message, List.of());
  }
}

package com.ulvratech.pratoemdia.server.features.profiles.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.InvalidProfileFieldsException;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotAvailableException;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.profiles.presentation")
public class ProfileExceptionHandler {
    @ExceptionHandler(ProfileNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(ProfileNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getCode(), "Perfil não encontrado.");
    }

    @ExceptionHandler(ProfileNotAvailableException.class)
    ResponseEntity<ApiErrorResponse> handleNotAvailable(ProfileNotAvailableException exception) {
        return response(
            HttpStatus.FORBIDDEN,
            exception.getCode(),
            "Este tipo de conta não possui perfil editável."
        );
    }

    @ExceptionHandler(InvalidProfileFieldsException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidFields(InvalidProfileFieldsException exception) {
        return response(
            HttpStatus.BAD_REQUEST,
            exception.getCode(),
            "Um ou mais campos não pertencem a este tipo de perfil."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidValue() {
        return response(
            HttpStatus.BAD_REQUEST,
            "PROFILE_INVALID_VALUE",
            "Um ou mais valores do perfil são inválidos."
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
        HttpStatus status,
        String code,
        String message
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            status.value(), code, message
        ));
    }
}

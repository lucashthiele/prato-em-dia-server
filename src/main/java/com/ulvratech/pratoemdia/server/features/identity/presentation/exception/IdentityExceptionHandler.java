package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(UserAccountAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAccountAlreadyExists(
        UserAccountAlreadyExistsException exception
    ) {
        var response = new ApiErrorResponse(
            HttpStatus.CONFLICT.value(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }
}
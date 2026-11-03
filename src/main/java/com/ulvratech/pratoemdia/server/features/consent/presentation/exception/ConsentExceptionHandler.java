package com.ulvratech.pratoemdia.server.features.consent.presentation.exception;

import com.ulvratech.pratoemdia.server.features.consent.application.exception.ConsentVersionMismatchException;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.consent.presentation")
public class ConsentExceptionHandler {
    @ExceptionHandler(ConsentVersionMismatchException.class)
    ResponseEntity<ApiErrorResponse> versionMismatch(ConsentVersionMismatchException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        HttpStatus.CONFLICT.value(), e.getCode(),
                        "A versão informada não é a vigente. Recarregue os documentos e aceite novamente."
                )
        );
    }
}

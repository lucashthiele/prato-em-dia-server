package com.ulvratech.pratoemdia.server.features.catalog.presentation.exception;

import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.catalog.application.exception.FoodNotFoundException;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.catalog.presentation")
public class CatalogExceptionHandler {
    @ExceptionHandler(FoodNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(FoodNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getCode(), "Alimento não encontrado.");
    }

    @ExceptionHandler(FoodAccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> forbidden(FoodAccessDeniedException exception) {
        return response(
                HttpStatus.FORBIDDEN,
                exception.getCode(),
                "Somente o autor ou um administrador pode alterar este alimento."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> invalid() {
        return response(
                HttpStatus.BAD_REQUEST, "CATALOG_INVALID_VALUE", "Um ou mais valores são inválidos."
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message));
    }
}

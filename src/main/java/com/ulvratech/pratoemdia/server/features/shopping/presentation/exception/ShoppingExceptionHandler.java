package com.ulvratech.pratoemdia.server.features.shopping.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingListNotFoundException;
import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingPlanNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.shopping.presentation")
public class ShoppingExceptionHandler {
    @ExceptionHandler(ShoppingListNotFoundException.class)
    ResponseEntity<ApiErrorResponse> listNotFound(ShoppingListNotFoundException exception) {
        return response(
                HttpStatus.NOT_FOUND, exception.getCode(), "O plano não tem uma lista de compras ativa."
        );
    }

    @ExceptionHandler(ShoppingPlanNotFoundException.class)
    ResponseEntity<ApiErrorResponse> planNotFound(ShoppingPlanNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getCode(), "O plano semanal não foi encontrado.");
    }

    @ExceptionHandler(ShoppingAccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> denied(ShoppingAccessDeniedException exception) {
        return response(
                HttpStatus.FORBIDDEN, exception.getCode(), "Somente o paciente dono do plano acessa a lista."
        );
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), code, message));
    }
}

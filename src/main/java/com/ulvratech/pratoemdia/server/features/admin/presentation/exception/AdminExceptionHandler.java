package com.ulvratech.pratoemdia.server.features.admin.presentation.exception;

import com.ulvratech.pratoemdia.server.features.admin.application.exception.AccessEmailNotAvailableException;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.AdminCannotChangeOwnStatusException;
import com.ulvratech.pratoemdia.server.features.admin.application.exception.EndRelationshipsNotApplicableException;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.admin.presentation")
public class AdminExceptionHandler {
    @ExceptionHandler(AccessEmailNotAvailableException.class)
    ResponseEntity<ApiErrorResponse> accessEmailNotAvailable(AccessEmailNotAvailableException e) {
        return conflict(
                e.getCode(),
                "O acesso só pode ser reenviado a um nutricionista ativo que ainda não trocou a senha inicial."
        );
    }

    @ExceptionHandler(AdminCannotChangeOwnStatusException.class)
    ResponseEntity<ApiErrorResponse> ownStatus(AdminCannotChangeOwnStatusException e) {
        return conflict(e.getCode(), "O administrador não pode alterar o status da própria conta.");
    }

    @ExceptionHandler(EndRelationshipsNotApplicableException.class)
    ResponseEntity<ApiErrorResponse> endRelationshipsNotApplicable(EndRelationshipsNotApplicableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(), e.getCode(),
                        "Encerrar vínculos só vale ao desativar uma conta de nutricionista."
                )
        );
    }

    private static ResponseEntity<ApiErrorResponse> conflict(String code, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(HttpStatus.CONFLICT.value(), code, message));
    }
}

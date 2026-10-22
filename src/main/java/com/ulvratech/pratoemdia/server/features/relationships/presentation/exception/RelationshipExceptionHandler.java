package com.ulvratech.pratoemdia.server.features.relationships.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipConflictException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipEmailException;
import com.ulvratech.pratoemdia.server.features.relationships.application.exception.RelationshipNotFoundException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.relationships.presentation")
public class RelationshipExceptionHandler {
    @ExceptionHandler(RelationshipNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(RelationshipNotFoundException e) {
        return response(
                HttpStatus.NOT_FOUND, e.getCode(), "O vínculo solicitado não foi encontrado."
        );
    }

    @ExceptionHandler(RelationshipConflictException.class)
    ResponseEntity<ApiErrorResponse> conflict(RelationshipConflictException e) {
        return response(
                HttpStatus.CONFLICT,
                e.getCode(),
                "O paciente já possui vínculo ativo ou o vínculo não pode ser alterado."
        );
    }

    @ExceptionHandler(RelationshipEmailException.class)
    ResponseEntity<ApiErrorResponse> email(RelationshipEmailException e) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                e.getCode(),
                "Não foi possível enviar o e-mail de acesso."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> invalid() {
        return response(
                HttpStatus.BAD_REQUEST,
                "RELATIONSHIP_INVALID_VALUE",
                "Um ou mais parâmetros da listagem são inválidos."
        );
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus s, String c, String m) {
        return ResponseEntity.status(s).body(new ApiErrorResponse(s.value(), c, m, List.of()));
    }
}

package com.ulvratech.pratoemdia.server.features.planning.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.InvalidPlanningChangeException;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningConflictException;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.WeeklyPlanNotFoundException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ulvratech.pratoemdia.server.features.planning.presentation")
public class PlanningExceptionHandler {
    @ExceptionHandler(WeeklyPlanNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound() {
        return response(
                HttpStatus.NOT_FOUND,
                "WEEKLY_PLAN_NOT_FOUND",
                "O plano semanal solicitado não foi encontrado."
        );
    }

    @ExceptionHandler(PlanningAccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> denied() {
        return response(
                HttpStatus.FORBIDDEN, "PLANNING_ACCESS_DENIED", "Você não tem acesso a este planejamento."
        );
    }

    @ExceptionHandler(PlanningConflictException.class)
    ResponseEntity<ApiErrorResponse> conflict(PlanningConflictException exception) {
        return response(HttpStatus.CONFLICT, "PLANNING_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler({ InvalidPlanningChangeException.class, IllegalArgumentException.class })
    ResponseEntity<ApiErrorResponse> invalid(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PLANNING", exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message, List.of()));
    }
}

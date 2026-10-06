package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import com.ulvratech.pratoemdia.server.features.identity.application.exception.ExpiredRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InactiveUserAccountException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InitialPasswordNotAvailableException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.PatientAccountConflictException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiFieldError;
import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class IdentityExceptionHandler {

    @ExceptionHandler(UserAccountAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAccountAlreadyExists(
            UserAccountAlreadyExistsException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                exception.getCode(),
                "Já existe uma conta cadastrada com este e-mail."
        );
    }

    @ExceptionHandler({
            PatientAccountConflictException.class,
            InitialPasswordNotAvailableException.class
    })
    public ResponseEntity<ApiErrorResponse> handlePatientAccountConflict(
            CodedApplicationException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                exception.getCode(),
                "A conta do paciente não está disponível para esta operação."
        );
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            InactiveUserAccountException.class,
            InvalidRefreshTokenException.class,
            ExpiredRefreshTokenException.class
    })
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(
            CodedApplicationException exception
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                exception.getCode(),
                authenticationFailureMessage(exception)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException exception
    ) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(
                        error -> new ApiFieldError(
                                error.getField(),
                                validationCode(error.getCode()),
                                validationMessage(error.getCode())
                        )
                )
                .toList();

        return ResponseEntity.badRequest()
                .body(
                        new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                "Existem campos inválidos na requisição.",
                                errors
                        )
                );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        var errors = exception.getConstraintViolations().stream()
                .map(
                        violation -> new ApiFieldError(
                                violation.getPropertyPath().toString(),
                                validationCode(
                                        violation
                                                .getConstraintDescriptor()
                                                .getAnnotation()
                                                .annotationType()
                                                .getSimpleName()
                                ),
                                validationMessage(
                                        violation
                                                .getConstraintDescriptor()
                                                .getAnnotation()
                                                .annotationType()
                                                .getSimpleName()
                                )
                        )
                )
                .toList();

        return ResponseEntity.badRequest()
                .body(
                        new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                "Existem campos inválidos na requisição.",
                                errors
                        )
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody() {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "O corpo da requisição está ausente ou possui JSON inválido."
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequestParameter() {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_PARAMETER",
                "Um dos parâmetros da requisição está ausente ou é inválido."
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRouteNotFound() {
        return response(
                HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "A rota solicitada não foi encontrada."
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed() {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "O método HTTP não é permitido para esta rota."
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType() {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "O formato do conteúdo enviado não é suportado."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedFailure(Exception exception) {
        log.error("Unexpected request failure", exception);
        return internalErrorResponse();
    }

    @ExceptionHandler(CodedApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledApplicationFailure(
            CodedApplicationException exception
    ) {
        log.error("Unhandled application failure. code={}", exception.getCode(), exception);
        return internalErrorResponse();
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status, String code, String message
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message));
    }

    private String validationCode(String constraintCode) {
        if (constraintCode == null) {
            return "INVALID_VALUE";
        }

        return switch (constraintCode) {
            case "NotBlank", "NotEmpty", "NotNull" -> "REQUIRED_FIELD";
            case "Email" -> "INVALID_EMAIL";
            case "Size" -> "INVALID_SIZE";
            case "Pattern" -> "INVALID_FORMAT";
            default -> "INVALID_VALUE";
        };
    }

    private String validationMessage(String constraintCode) {
        if (constraintCode == null) {
            return "O valor informado é inválido.";
        }

        return switch (constraintCode) {
            case "NotBlank", "NotEmpty", "NotNull" -> "Este campo é obrigatório.";
            case "Email" -> "Informe um e-mail válido.";
            case "Size" -> "O tamanho informado é inválido.";
            case "Pattern" -> "O formato informado é inválido.";
            case "EnumValue" -> "O valor informado não é permitido.";
            default -> "O valor informado é inválido.";
        };
    }

    private String authenticationFailureMessage(CodedApplicationException exception) {
        return switch (exception.getCode()) {
            case "AUTH_INVALID_CREDENTIALS" -> "E-mail ou senha inválidos.";
            case "AUTH_ACCOUNT_INACTIVE" -> "Esta conta está inativa.";
            case "AUTH_REFRESH_TOKEN_INVALID" -> "O token de renovação é inválido ou foi revogado.";
            case "AUTH_REFRESH_TOKEN_EXPIRED" -> "O token de renovação expirou.";
            default -> "Não foi possível autenticar a solicitação.";
        };
    }

    private ResponseEntity<ApiErrorResponse> internalErrorResponse() {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Não foi possível concluir a operação. Tente novamente mais tarde."
        );
    }
}

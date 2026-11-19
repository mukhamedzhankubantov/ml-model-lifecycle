package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.domain.DuplicateModel;
import com.mlops.modellifecycle.domain.ModelNotFound;
import com.mlops.modellifecycle.domain.PolicyViolation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "request is not valid";
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badPathId(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", "id must be a UUID", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> badBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", "body must be JSON", request);
    }

    // Login only: unknown user and wrong password must look identical.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> badLogin(AuthenticationException ex, HttpServletRequest request) {
        log.warn("login failed");
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "bad username or password", request);
    }

    @ExceptionHandler(ModelNotFound.class)
    public ResponseEntity<ApiError> notFound(ModelNotFound ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(PolicyViolation.class)
    public ResponseEntity<ApiError> policyViolation(PolicyViolation ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "POLICY_VIOLATION", ex.getMessage(), request);
    }

    // Defense in depth: @PreAuthorize throws inside the controller call, where the
    // filter-level handler does not see it. Same 403 body as the URL rule.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "not allowed for your role", request);
    }

    @ExceptionHandler(DuplicateModel.class)
    public ResponseEntity<ApiError> duplicate(DuplicateModel ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> unsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED", "content type must be application/json", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "resource not found", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        String correlationId = correlationId(request);
        log.error("unexpected error, correlationId={}", correlationId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL", "unexpected error", correlationId));
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(code, message, correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        return CorrelationIds.resolve(request);
    }
}
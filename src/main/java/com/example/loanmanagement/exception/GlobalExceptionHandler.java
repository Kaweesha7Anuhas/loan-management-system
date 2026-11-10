package com.example.loanmanagement.exception;

import com.example.loanmanagement.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling for ALL controllers.
 *
 * Design goals:
 *  1. Controllers never write try/catch — they just throw.
 *  2. Every error response has the same JSON shape (ApiError).
 *  3. HTTP status matches the failure semantics (4xx vs 5xx, specific codes).
 *  4. Stack traces, SQL strings, and class names NEVER leak to the client.
 *  5. Logging level is appropriate: WARN for business failures, ERROR for
 *     unexpected server-side failures.
 *
 * Handled exception categories:
 *
 *  ┌──────────────────────────────────────┬────────┬─────────────────────┐
 *  │ Exception                             │ Status │ Cause                │
 *  ├──────────────────────────────────────┼────────┼─────────────────────┤
 *  │ ApiException (base + all subclasses) │  varies│ Typed business failures │
 *  │ MethodArgumentNotValidException      │  400   │ @Valid on @RequestBody │
 *  │ ConstraintViolationException         │  400   │ @Validated on method params │
 *  │ HttpMessageNotReadableException      │  400   │ Malformed JSON       │
 *  │ MissingServletRequestParameter       │  400   │ Missing query param  │
 *  │ MethodArgumentTypeMismatch           │  400   │ "abc" sent for Long  │
 *  │ AccessDeniedException                │  403   │ Spring Security      │
 *  │ AuthenticationException              │  401   │ Bad credentials      │
 *  │ DataIntegrityViolationException      │  409   │ DB unique/FK clash   │
 *  │ Exception (fallback)                 │  500   │ Anything unexpected  │
 *  └──────────────────────────────────────┴────────┴─────────────────────┘
 *
 * NOTE: 401/403 that occur in the Spring Security FILTER chain
 * (before the request reaches any controller) are NOT caught here — they're
 * handled by a custom AuthenticationEntryPoint / AccessDeniedHandler which
 * we'll configure in Phase 4.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─────────────────────────────────────────────
    // Our own typed exceptions
    // ─────────────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("[{}] {} — {}", ex.getStatus().value(), request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────
    // Jakarta Bean Validation
    // ─────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[400] Validation failed at {} — {}", request.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Validation failed", request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String path = cv.getPropertyPath().toString();
            String field = path.contains(".")
                    ? path.substring(path.lastIndexOf('.') + 1)
                    : path;
            fieldErrors.putIfAbsent(field, cv.getMessage());
        });
        log.warn("[400] Constraint violation at {} — {}", request.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Validation failed", request.getRequestURI(), fieldErrors);
    }

    // ─────────────────────────────────────────────
    // Malformed / missing request data
    // ─────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[400] Malformed request body at {} — {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Malformed request body", request.getRequestURI(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("[400] {}", msg);
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                msg, request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "Parameter '" + ex.getName() + "' has an invalid value";
        log.warn("[400] {}", msg);
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                msg, request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────
    // Spring Security (thrown from @PreAuthorize / auth attempts)
    // ─────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[403] Access denied at {}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("[401] Authentication failed at {} — {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Invalid credentials", request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────
    // Database constraints (duplicate NIC, FK violations)
    // ─────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        // Log the real cause for debugging; return a generic message.
        log.error("[409] Data integrity violation at {}", request.getRequestURI(), ex);
        String msg = "The operation conflicts with an existing record or violates a constraint";
        return build(HttpStatus.CONFLICT, "Conflict",
                msg, request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────
    // Fallback — never leak internal detail to the client
    // ─────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("[500] Unexpected exception at {}", request.getRequestURI(), ex);
        String msg = "An unexpected error occurred. Please contact support.";
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                msg, request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────

    private ResponseEntity<ApiError> build(
            HttpStatus status, String error, String message,
            String path, Map<String, String> fieldErrors) {

        ApiError body = ApiError.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
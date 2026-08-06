package com.orman.backend.common.error;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.exception.LastOwnerRequiredException;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.exception.ExpiredJwtException;
import com.orman.backend.auth.exception.ExpiredSessionException;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.exception.RevokedSessionException;
import com.orman.backend.auth.exception.SecurityAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS,
                "Credenciales inválidas", "Las credenciales no son válidas.", request, List.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ProblemDetail> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN,
                "Sesión no válida", "La sesión no es válida o ha expirado.", request, List.of());
    }

    @ExceptionHandler(InvalidJwtException.class)
    ResponseEntity<ProblemDetail> handleInvalidJwt(InvalidJwtException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN,
                "Token no válido", "El access token no es válido.", request, List.of());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    ResponseEntity<ProblemDetail> handleExpiredJwt(ExpiredJwtException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED,
                "Token expirado", "El access token ha expirado.", request, List.of());
    }

    @ExceptionHandler(RevokedSessionException.class)
    ResponseEntity<ProblemDetail> handleRevokedSession(
            RevokedSessionException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.SESSION_REVOKED,
                "Sesión revocada", "La sesión fue revocada.", request, List.of());
    }

    @ExceptionHandler(ExpiredSessionException.class)
    ResponseEntity<ProblemDetail> handleExpiredSession(
            ExpiredSessionException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.SESSION_EXPIRED,
                "Sesión expirada", "La sesión ha expirado.", request, List.of());
    }

    @ExceptionHandler(SecurityAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleSecurityAccessDenied(
            SecurityAccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.INVALID_REQUEST,
                "Solicitud rechazada", "La solicitud de seguridad no es válida.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED,
                "Acceso denegado", "No tiene autorización para realizar esta operación.", request, List.of());
    }

    @ExceptionHandler(LastOwnerRequiredException.class)
    ResponseEntity<ProblemDetail> handleLastOwnerRequired(
            LastOwnerRequiredException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ErrorCode.LAST_OWNER_REQUIRED,
                "Propietario requerido", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                "Recurso no encontrado", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                "Conflicto", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ProblemDetail> handleBusinessRule(
            BusinessRuleException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.BUSINESS_RULE_VIOLATION,
                "Regla de negocio no cumplida", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), defaultMessage(error.getDefaultMessage())))
                .distinct()
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList();

        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Solicitud no válida", "Uno o más campos no son válidos.", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<FieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldError(lastPathSegment(violation), violation.getMessage()))
                .distinct()
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList();

        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Solicitud no válida", "Uno o más parámetros no son válidos.", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                "Solicitud no válida", "El cuerpo de la solicitud no es válido.", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.INVALID_REQUEST,
                "Método no permitido", "El método HTTP no está permitido para este recurso.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        LOGGER.error("Unexpected error traceId={} path={} exceptionType={}", traceId,
                request.getRequestURI(), exception.getClass().getSimpleName());

        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Error interno", "Ocurrió un error interno.", request, List.of(), traceId);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode errorCode, String title,
            String detail, HttpServletRequest request, List<FieldError> fieldErrors) {
        return problem(status, errorCode, title, detail, request, fieldErrors, UUID.randomUUID().toString());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode errorCode, String title,
            String detail, HttpServletRequest request, List<FieldError> fieldErrors, String traceId) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("errorCode", errorCode.name());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("traceId", traceId);
        if (!fieldErrors.isEmpty()) {
            problemDetail.setProperty("fieldErrors", fieldErrors);
        }

        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body(problemDetail);
    }

    private String defaultMessage(String message) {
        return message == null ? "Valor no válido." : message;
    }

    private String lastPathSegment(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }

    private record FieldError(String field, String message) {
    }
}

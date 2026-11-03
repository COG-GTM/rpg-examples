package com.example.clients.api;

import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Maps errors to RFC 7807 {@code application/problem+json} with an extra {@code code}
 * property: INVDNI→400, VALIDATION→400, NOTFND→404, DUPDNI→409, DELETED→410, IOERR→500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String VALIDATION = "VALIDATION";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ClientException.class)
    ResponseEntity<Object> handleClient(ClientException ex, WebRequest request) {
        if (ex.code() == ClientErrorCode.IOERR || ex.code() == ClientErrorCode.INVOP) {
            log.error("Client operation failed with {}", ex.code(), ex);
        }
        return problem(statusOf(ex.code()), ex.code().name(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ClientErrorCode.IOERR.name(),
                "Unexpected server error", request);
    }

    /** Constraint violations on {@code @ValidDni} path variables and/or {@code @Valid} bodies. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        boolean invalidDni = ex.getAllValidationResults().stream()
                .anyMatch(r -> r.getMethodParameter().hasParameterAnnotation(PathVariable.class));
        if (invalidDni) {
            return invalidDni(request);
        }
        String detail = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, VALIDATION, detail, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String detail = ex.getBindingResult().getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, VALIDATION, detail, request);
    }

    /** Non-numeric or overflowing {@code {dni}} path segment. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return invalidDni(request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, VALIDATION, "Request body is missing or malformed", request);
    }

    private ResponseEntity<Object> invalidDni(WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ClientErrorCode.INVDNI.name(),
                ClientErrorCode.INVDNI.defaultMessage(), request);
    }

    static HttpStatus statusOf(ClientErrorCode code) {
        return switch (code) {
            case INVDNI -> HttpStatus.BAD_REQUEST;
            case NOTFND -> HttpStatus.NOT_FOUND;
            case DUPDNI -> HttpStatus.CONFLICT;
            case DELETED -> HttpStatus.GONE;
            case OK, IOERR, INVOP -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String code, String detail, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("code", code);
        if (request instanceof ServletWebRequest servlet) {
            body.setInstance(URI.create(servlet.getRequest().getRequestURI()));
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}

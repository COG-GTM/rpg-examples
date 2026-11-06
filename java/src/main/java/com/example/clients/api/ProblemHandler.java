package com.example.clients.api;

import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.Dni;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every failure to RFC 7807 {@code application/problem+json} with an extra {@code code}
 * property. For CLIESRV codes {@code detail} carries the legacy message.
 */
@RestControllerAdvice
public class ProblemHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);

    @ExceptionHandler(ClientException.class)
    ResponseEntity<Object> handleClient(ClientException ex, HttpServletRequest request) {
        if (ex.code().status().is5xxServerError()) {
            log.error("{} on {} {}: {}", ex.code(), request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        }
        return problem(ex.code(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(ClientErrorCode.IOERR, ClientErrorCode.IOERR.legacyMessage(), request.getRequestURI());
    }

    /** Path-variable constraints (and, alongside them, body constraints) fail here; INVDNI wins. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        boolean dniInvalid = ex.getAllValidationResults().stream()
                .anyMatch(r -> ClientController.DNI.equals(r.getMethodParameter().getParameterName()));
        if (dniInvalid) {
            return problem(ClientErrorCode.INVDNI, ClientErrorCode.INVDNI.legacyMessage(), uri(request));
        }
        String detail = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream().map(e -> describe(e)))
                .collect(Collectors.joining("; "));
        return problem(ClientErrorCode.VALIDATION, detail, uri(request));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream().map(ProblemHandler::describe).toList();
        return problem(ClientErrorCode.VALIDATION, String.join("; ", errors), uri(request));
    }

    /** Non-numeric or overflowing {@code {dni}}. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (ClientController.DNI.equals(ex.getPropertyName())) {
            return problem(ClientErrorCode.INVDNI, ClientErrorCode.INVDNI.legacyMessage(), uri(request));
        }
        return problem(ClientErrorCode.VALIDATION, "Invalid value for " + ex.getPropertyName(), uri(request));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (pathDniInvalid(request)) {
            return problem(ClientErrorCode.INVDNI, ClientErrorCode.INVDNI.legacyMessage(), uri(request));
        }
        return problem(ClientErrorCode.VALIDATION, "Request body must be JSON like {\"name\": \"...\"}", uri(request));
    }

    /** The body is read before method validation runs, so re-check the path DNI to keep INVDNI first. */
    private static boolean pathDniInvalid(WebRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (!(vars instanceof Map<?, ?> map) || !(map.get(ClientController.DNI) instanceof String raw)) {
            return false;
        }
        if (!ClientController.DECIMAL.matcher(raw).matches()) {
            return true;
        }
        try {
            return !Dni.isValid(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static String describe(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return error instanceof FieldError fe ? fe.getField() + ": " + message : "name: " + message;
    }

    private static String uri(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
    }

    static ResponseEntity<Object> problem(ClientErrorCode code, String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setTitle(code.title());
        pd.setProperty(CODE, code.name());
        if (instance != null) {
            pd.setInstance(URI.create(instance));
        }
        return ResponseEntity.status(code.status()).body(pd);
    }
}

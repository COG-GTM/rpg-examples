package com.example.clients.api;

import com.example.clients.domain.ClientException;
import com.example.clients.domain.ClientsvCode;
import com.example.clients.domain.Dni;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every failure is RFC 7807 {@code application/problem+json} with an extra {@code code} property
 * (the CLIENTSV result code). For CLIENTSV failures {@code detail} is the RPG message.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final Pattern CLIENT_PATH = Pattern.compile("^/clients/([^/]*)/?$");

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
        return problem(ClientsvCode.IOERR, ClientsvCode.IOERR.legacyMessage(), request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Body binding runs before the controller, so re-apply the DNI-first guard order here.
        String path = uri(request);
        Matcher m = path == null ? null : CLIENT_PATH.matcher(path);
        if (m != null && m.matches()) {
            try {
                Dni.parse(m.group(1));
            } catch (ClientException invalid) {
                return problem(invalid.code(), invalid.getMessage(), path);
            }
        }
        return problem(ClientsvCode.VALIDATION, "Request body must be JSON like {\"name\": \"...\"}", uri(request));
    }

    private static String uri(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
    }

    static ResponseEntity<Object> problem(ClientsvCode code, String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setTitle(code.title());
        pd.setProperty("code", code.name());
        if (instance != null) {
            pd.setInstance(URI.create(instance));
        }
        return ResponseEntity.status(code.status()).body(pd);
    }
}

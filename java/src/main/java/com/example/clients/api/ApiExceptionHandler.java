package com.example.clients.api;

import com.example.clients.domain.ClientException;
import com.example.clients.domain.ClientsvCode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
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

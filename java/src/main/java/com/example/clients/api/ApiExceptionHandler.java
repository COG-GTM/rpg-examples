package com.example.clients.api;

import com.example.clients.domain.ClientOperationException;
import com.example.clients.domain.ClientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Maps CLIESRV status codes to HTTP. The RPG message is returned verbatim in {@code detail}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ClientOperationException.class)
    ProblemDetail handle(ClientOperationException e) {
        return problem(httpStatus(e.status()), e.status(), e.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handle(HandlerMethodValidationException e) {
        String detail = e.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(err -> err.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return problem(HttpStatus.BAD_REQUEST, ClientStatus.INVALID_DNI, detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handle(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Invalid request");
        return pd;
    }

    static HttpStatus httpStatus(ClientStatus status) {
        return switch (status) {
            case OK -> HttpStatus.OK;
            case INVALID_DNI -> HttpStatus.BAD_REQUEST;
            case DUPLICATE -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DELETED -> HttpStatus.GONE;
            case IO_ERROR, INVALID_OP -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ProblemDetail problem(HttpStatus http, ClientStatus code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(http, detail);
        pd.setTitle(code.name());
        pd.setProperty("code", code.rpgCode());
        return pd;
    }
}

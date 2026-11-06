package com.example.clients.domain;

import org.springframework.http.HttpStatus;

/** CLIESRV status codes (Contract A) plus the API-only VALIDATION code, with their HTTP mapping (Contract B). */
public enum ClientErrorCode {
    INVDNI(HttpStatus.BAD_REQUEST, "Invalid DNI", "DNI not valid"),
    VALIDATION(HttpStatus.BAD_REQUEST, "Validation failed", "Request validation failed"),
    NOTFND(HttpStatus.NOT_FOUND, "Client not found", "DNI not exists"),
    DUPDNI(HttpStatus.CONFLICT, "Duplicate DNI", "DNI already exists"),
    DELETED(HttpStatus.GONE, "Client deleted", "Client marked as deleted"),
    IOERR(HttpStatus.INTERNAL_SERVER_ERROR, "I/O error", "I/O error on TRCLIE"),
    INVOP(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid operation", "OP. Error");

    private final HttpStatus status;
    private final String title;
    private final String legacyMessage;

    ClientErrorCode(HttpStatus status, String title, String legacyMessage) {
        this.status = status;
        this.title = title;
        this.legacyMessage = legacyMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** Legacy CLIECRUD DSPLY wording, used as the problem {@code detail}. */
    public String legacyMessage() {
        return legacyMessage;
    }

    /** Maps a raw CLIESRV status code; unknown codes become IOERR. */
    public static ClientErrorCode fromStatusCode(String code) {
        String c = code == null ? "" : code.trim();
        for (ClientErrorCode value : values()) {
            if (value != VALIDATION && value.name().equals(c)) {
                return value;
            }
        }
        return IOERR;
    }
}

package com.example.clients.domain;

import org.springframework.http.HttpStatus;

/** Result codes of CLIENTSV ({@code clientsv_out_t.code}) plus API-only codes, with their HTTP mapping. */
public enum ClientsvCode {
    OK(HttpStatus.OK, "OK", ""),
    INVDNI(HttpStatus.BAD_REQUEST, "Invalid DNI", "DNI not valid"),
    DUPDNI(HttpStatus.CONFLICT, "Client already exists", "DNI already exists"),
    NOTFND(HttpStatus.NOT_FOUND, "Client not found", "DNI not exists"),
    DELETED(HttpStatus.GONE, "Client deleted", "Client marked as deleted"),
    INVOP(HttpStatus.BAD_REQUEST, "Invalid operation", "OP. Error"),
    IOERR(HttpStatus.SERVICE_UNAVAILABLE, "Client store unavailable", "I/O error on TRCLIE"),
    /** API-only: request body failed validation (e.g. blank name). */
    VALIDATION(HttpStatus.BAD_REQUEST, "Invalid request", "Invalid request");

    private final HttpStatus status;
    private final String title;
    private final String legacyMessage;

    ClientsvCode(HttpStatus status, String title, String legacyMessage) {
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

    public String legacyMessage() {
        return legacyMessage;
    }

    /** Unknown codes coming back from IBM i are treated as I/O errors. */
    public static ClientsvCode fromWire(String code) {
        if (code == null) {
            return IOERR;
        }
        try {
            return valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            return IOERR;
        }
    }
}

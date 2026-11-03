package com.example.clients.domain;

import java.util.Arrays;

/** CLIESRV status codes ({@code cliesrv_status_t.code}). */
public enum ClientErrorCode {
    OK("OK"),
    INVDNI("DNI not valid"),
    DUPDNI("DNI already exists"),
    NOTFND("DNI not exists"),
    DELETED("Client marked as deleted"),
    IOERR("I/O error"),
    INVOP("Invalid operation");

    private final String defaultMessage;

    ClientErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /** Parses a (blank padded) RPG code; unknown values map to {@link #IOERR}. */
    public static ClientErrorCode fromRpg(String code) {
        String trimmed = code == null ? "" : code.trim();
        return Arrays.stream(values())
                .filter(c -> c.name().equals(trimmed))
                .findFirst()
                .orElse(IOERR);
    }
}

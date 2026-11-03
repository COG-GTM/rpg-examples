package com.example.clients.domain;

import java.util.Arrays;

/**
 * Status codes returned by CLIESRV in {@code cliesrv_status_t.code} (see cliesrv_p.rpgle).
 */
public enum ClientStatus {
    OK("OK"),
    INVALID_DNI("INVDNI"),
    DUPLICATE("DUPDNI"),
    NOT_FOUND("NOTFND"),
    DELETED("DELETED"),
    IO_ERROR("IOERR"),
    INVALID_OP("INVOP");

    private final String rpgCode;

    ClientStatus(String rpgCode) {
        this.rpgCode = rpgCode;
    }

    public String rpgCode() {
        return rpgCode;
    }

    public static ClientStatus fromRpgCode(String code) {
        String trimmed = code == null ? "" : code.trim();
        return Arrays.stream(values())
                .filter(s -> s.rpgCode.equals(trimmed))
                .findFirst()
                .orElse(IO_ERROR);
    }
}

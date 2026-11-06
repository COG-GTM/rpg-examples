package com.example.clients.domain;

/** A non-OK CLIESRV status (or its in-memory equivalent). */
public class ClientException extends RuntimeException {

    private final ClientErrorCode code;

    public ClientException(ClientErrorCode code) {
        this(code, code.legacyMessage());
    }

    public ClientException(ClientErrorCode code, String message) {
        super(message == null || message.isBlank() ? code.legacyMessage() : message.trim());
        this.code = code;
    }

    public ClientException(ClientErrorCode code, String message, Throwable cause) {
        super(message == null || message.isBlank() ? code.legacyMessage() : message.trim(), cause);
        this.code = code;
    }

    public ClientErrorCode code() {
        return code;
    }
}

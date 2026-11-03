package com.example.clients.domain;

/** A CLIESRV procedure rejected or failed the operation. */
public class ClientException extends RuntimeException {

    private final ClientErrorCode code;

    public ClientException(ClientErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ClientException(ClientErrorCode code, String message) {
        this(code, message, null);
    }

    public ClientException(ClientErrorCode code, String message, Throwable cause) {
        super(message == null || message.isBlank() ? code.defaultMessage() : message, cause);
        this.code = code;
    }

    public ClientErrorCode code() {
        return code;
    }
}

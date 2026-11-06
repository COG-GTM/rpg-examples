package com.example.clients.domain;

/** A CLIENTSV failure (or API validation failure) that maps to an HTTP problem response. */
public class ClientException extends RuntimeException {

    private final ClientsvCode code;

    public ClientException(ClientsvCode code, String message) {
        super(message);
        this.code = code;
    }

    public ClientException(ClientsvCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ClientsvCode code() {
        return code;
    }
}

package com.example.clients.domain;

/**
 * A CLIESRV operation was rejected (success flag off). Carries the RPG status code and message.
 */
public class ClientOperationException extends RuntimeException {

    private final ClientStatus status;

    public ClientOperationException(ClientStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ClientOperationException(ClientStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public ClientStatus status() {
        return status;
    }
}

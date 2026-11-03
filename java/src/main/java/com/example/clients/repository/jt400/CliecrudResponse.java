package com.example.clients.repository.jt400;

/** Raw output parameters 4 (status, 63 bytes) and 5 (client, 146 bytes) of CLIECRUD. */
public record CliecrudResponse(byte[] status, byte[] client) {
}

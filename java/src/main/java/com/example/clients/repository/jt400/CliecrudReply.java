package com.example.clients.repository.jt400;

/** Raw output parameters of a 5-parm CLIECRUD call: status (63 bytes) and client image (146 bytes). */
public record CliecrudReply(byte[] status, byte[] client) {
}

package com.example.clients.repository.jt400;

/** Decoded {@code cliesrv_status_t} (63 bytes: success ind 1, code char 10, message char 52). */
public record CliesrvStatus(boolean success, String code, String message) {
}

package com.example.clients.repository.jt400;

import com.example.clients.domain.ClientErrorCode;

/** Decoded {@code cliesrv_status_t} (63 bytes). */
public record CliesrvStatus(boolean success, String code, String message) {

    public ClientErrorCode errorCode() {
        return ClientErrorCode.fromRpg(code);
    }
}

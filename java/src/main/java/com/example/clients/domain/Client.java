package com.example.clients.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * One TRCLIE record (RETRCLIE format). {@code *At} fields are null when the RPG
 * timestamp is 0001-01-01; {@code *By} fields are null when blank.
 */
@JsonPropertyOrder({"dni", "name", "createdAt", "createdBy", "updatedAt", "updatedBy",
        "deletedAt", "deletedBy", "active"})
public record Client(
        long dni,
        String name,
        @JsonFormat(pattern = Client.TS_PATTERN) LocalDateTime createdAt,
        String createdBy,
        @JsonFormat(pattern = Client.TS_PATTERN) LocalDateTime updatedAt,
        String updatedBy,
        @JsonFormat(pattern = Client.TS_PATTERN) LocalDateTime deletedAt,
        String deletedBy) {

    static final String TS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";

    /** Mirrors {@code clientActive}: a client is active while CLDEUS is blank. */
    @JsonProperty("active")
    public boolean active() {
        return deletedBy == null || deletedBy.isBlank();
    }
}

package com.example.clients.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * One TRCLIE record (RETRCLIE). {@code *At} fields are null for the {@code 0001-01-01} sentinel,
 * {@code *By} fields are trimmed and null when blank, and {@code active} means CLDEUS is blank.
 */
public record Client(
        @Min(Dni.MIN) @Max(Dni.MAX) long dni,
        @NotBlank @Size(max = 30) String name,
        @JsonFormat(pattern = Client.TS_PATTERN) LocalDateTime createdAt,
        String createdBy,
        @JsonFormat(pattern = Client.TS_PATTERN) LocalDateTime updatedAt,
        String updatedBy,
        @JsonFormat(pattern = Client.TS_PATTERN) LocalDateTime deletedAt,
        String deletedBy,
        boolean active) {

    public static final String TS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
}

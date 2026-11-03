package com.example.clients.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Client DTO, one row of TRCLIE (record RETRCLIE).
 *
 * @param dni       CLDNI
 * @param name      CLNAME
 * @param createdAt CLCRTS
 * @param createdBy CLCRUS
 * @param updatedAt CLUPTS (null when never updated)
 * @param updatedBy CLUPUS
 * @param deletedAt CLDETS (null while active)
 * @param deletedBy CLDEUS (blank while active)
 */
public record Client(
        @ValidDni long dni,
        @Size(max = ClientRules.NAME_MAX_LENGTH) String name,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime deletedAt,
        String deletedBy) {

    @JsonProperty("active")
    public boolean active() {
        return ClientRules.isActive(deletedBy);
    }
}

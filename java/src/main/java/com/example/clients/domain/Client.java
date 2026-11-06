package com.example.clients.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * JSON view of the CLIENTSV {@code client_t} data structure (record format RETRCLIE).
 * Audit timestamps/users are null when TRCLIE holds the "not set" value.
 *
 * <pre>
 * dni       = CLDNI    createdAt = CLCRTS   updatedAt = CLUPTS   deletedAt = CLDETS
 * name      = CLNAME   createdBy = CLCRUS   updatedBy = CLUPUS   deletedBy = CLDEUS
 * </pre>
 */
public record Client(
        long dni,
        String name,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime deletedAt,
        String deletedBy) {

    /** Same rule as CLIENTSV {@code clientActive}: no deletion user stamped. */
    @JsonProperty("active")
    public boolean active() {
        return deletedBy == null || deletedBy.isBlank();
    }
}

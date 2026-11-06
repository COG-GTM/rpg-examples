package com.example.clients.repository.jt400;

/**
 * One row of the TRCLIE list query. Timestamps are {@code CHAR(...)} columns in the 26-char *ISO
 * form {@code YYYY-MM-DD-HH.MM.SS.ffffff}, the same as in the program client image.
 */
public record TrclieRow(long dni, String name, String createdTs, String createdBy, String updatedTs,
        String updatedBy, String deletedTs, String deletedBy) {
}

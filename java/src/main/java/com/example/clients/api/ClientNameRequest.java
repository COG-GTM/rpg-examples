package com.example.clients.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST/PUT {@code /clients/{dni}}. CLNAME is char(30) EBCDIC (CCSID 37), whose repertoire is
 * Latin-1, so other characters are rejected in every repository mode instead of being substituted on IBM i.
 */
public record ClientNameRequest(
        @NotBlank(message = "name is required")
        @Size(max = 30, message = "name must be at most 30 characters")
        @Pattern(regexp = "[\\x20-\\x7E\\xA0-\\xFF]*", message = "name contains unsupported characters")
        String name) {
}

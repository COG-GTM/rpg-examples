package com.example.clients.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST/PUT /clients/{dni}. {@code name} is CLNAME char(30). */
public record ClientNameRequest(
        @NotBlank(message = "name is required") @Size(max = 30, message = "name must be at most 30 characters")
        String name) {
}

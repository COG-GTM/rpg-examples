package com.example.clients.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST/PUT {@code /clients/{dni}}. */
public record ClientNameRequest(@NotBlank @Size(max = 30) String name) {
}

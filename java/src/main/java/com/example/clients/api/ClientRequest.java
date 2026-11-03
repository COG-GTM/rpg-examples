package com.example.clients.api;

import com.example.clients.domain.ClientRules;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of POST/PUT /clients/{dni}. */
public record ClientRequest(@NotNull @Size(max = ClientRules.NAME_MAX_LENGTH) String name) {
}

package com.example.clients.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void dniBoundaries() {
        assertThat(validator.validate(client(999_999L))).extracting(v -> v.getMessage()).containsExactly("DNI not valid");
        assertThat(validator.validate(client(1_000_000L))).isEmpty();
        assertThat(validator.validate(client(99_999_999L))).isEmpty();
        assertThat(validator.validate(client(100_000_000L))).hasSize(1);
        assertThat(ClientRules.dniIsValid(1_000_000L)).isTrue();
        assertThat(ClientRules.dniIsValid(100_000_000L)).isFalse();
    }

    @Test
    void activeMeansDeletedByBlank() {
        assertThat(new Client(1_000_000L, "a", null, "", null, "", null, "  ").active()).isTrue();
        assertThat(new Client(1_000_000L, "a", null, "", null, "", null, "QPGMR").active()).isFalse();
    }

    private static Client client(long dni) {
        return new Client(dni, "Ana", null, "", null, "", null, "");
    }
}

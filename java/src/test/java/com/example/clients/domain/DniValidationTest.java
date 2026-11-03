package com.example.clients.domain;

import com.example.clients.api.ValidDni;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DniValidationTest {

    record Holder(@ValidDni Long dni) {
    }

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest(name = "dni {0} valid={1}")
    @CsvSource({
            "0, false",
            "-1, false",
            "999999, false",
            "1000000, true",
            "12345678, true",
            "99999999, true",
            "100000000, false"
    })
    void boundaries(long dni, boolean valid) {
        assertThat(Dni.isValid(dni)).isEqualTo(valid);
        assertThat(validator.validate(new Holder(dni)).isEmpty()).isEqualTo(valid);
    }

    @org.junit.jupiter.api.Test
    void nullIsRejectedWithLegacyMessage() {
        var violations = validator.validate(new Holder(null));
        assertThat(violations).singleElement()
                .satisfies(v -> assertThat(v.getMessage()).isEqualTo("DNI not valid"));
    }
}

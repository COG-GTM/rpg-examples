package com.example.clients.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.clients.api.ClientNameRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientValidationTest {

    private static ValidatorFactory factory;
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

    @ParameterizedTest
    @CsvSource({"999999,false", "1000000,true", "99999999,true", "100000000,false", "0,false", "-1000000,false"})
    void dniBoundaries(long dni, boolean valid) {
        Client client = new Client(dni, "ANA", null, null, null, null, null, null, true);
        assertThat(validator.validate(client).isEmpty()).isEqualTo(valid);
        assertThat(Dni.isValid(dni)).isEqualTo(valid);
    }

    @Test
    void nameRules() {
        assertThat(validator.validate(new ClientNameRequest("x".repeat(30)))).isEmpty();
        assertThat(validator.validate(new ClientNameRequest("x".repeat(31)))).hasSize(1);
        assertThat(validator.validate(new ClientNameRequest("   "))).hasSize(1);
        assertThat(validator.validate(new ClientNameRequest(null))).hasSize(1);
    }

    @Test
    void unknownStatusCodesMapToIoerr() {
        assertThat(ClientErrorCode.fromStatusCode("DELETED   ")).isEqualTo(ClientErrorCode.DELETED);
        assertThat(ClientErrorCode.fromStatusCode("WHAT")).isEqualTo(ClientErrorCode.IOERR);
        assertThat(ClientErrorCode.fromStatusCode("VALIDATION")).isEqualTo(ClientErrorCode.IOERR);
        assertThat(ClientErrorCode.fromStatusCode(null)).isEqualTo(ClientErrorCode.IOERR);
    }
}

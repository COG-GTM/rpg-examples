package com.example.clients.repository.jt400;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class Jt400PropertiesTest {

    @Test
    void dataLibraryDefaultsToProgramLibrary() {
        Jt400Properties p = new Jt400Properties("h", "u", "p", "CLIELIB", "", null);
        assertThat(p.effectiveDataLibrary()).isEqualTo("CLIELIB");
        assertThat(p.effectiveCcsid()).isEqualTo(37);
        assertThat(new Jt400Properties("h", "u", "p", "CLIELIB", "DATALIB", 500).effectiveDataLibrary())
                .isEqualTo("DATALIB");
    }

    @Test
    void validateListsMissingSettings() {
        assertThatThrownBy(() -> new Jt400Properties("", null, "", "L", null, null).validate())
                .hasMessage("CLIENTS_REPOSITORY=jt400 requires: IBMI_HOST IBMI_USER IBMI_PASSWORD");
        assertThatNoException().isThrownBy(() -> new Jt400Properties("h", "u", "p", "L", null, null).validate());
    }

    @Test
    void toStringMasksPassword() {
        assertThat(new Jt400Properties("h", "u", "s3cret", "L", null, null).toString())
                .doesNotContain("s3cret").contains("password=***");
    }
}

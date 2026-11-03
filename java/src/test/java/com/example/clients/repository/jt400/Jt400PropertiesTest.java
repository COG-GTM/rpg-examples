package com.example.clients.repository.jt400;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Jt400PropertiesTest {

    @Test
    void dataLibraryDefaultsToProgramLibraryAndIsUppercased() {
        Jt400Properties p = new Jt400Properties("host", "user", "pw", "mylib", "", 37);
        p.validate();
        assertThat(p.programLibrary()).isEqualTo("MYLIB");
        assertThat(p.fileLibrary()).isEqualTo("MYLIB");
    }

    @Test
    void missingOrInvalidSettingsFailFast() {
        assertThatThrownBy(() -> new Jt400Properties("", "u", "p", "LIB", null, 37).validate())
                .hasMessageContaining("IBMI_HOST");
        assertThatThrownBy(() -> new Jt400Properties("h", "u", "", "LIB", null, 37).validate())
                .hasMessageContaining("IBMI_PASSWORD");
        assertThatThrownBy(() -> new Jt400Properties("h", "u", "p", "LIB) X(", null, 37).validate())
                .hasMessageContaining("not a valid IBM i object name");
        assertThatThrownBy(() -> new Jt400Properties("h", "u", "p", "LIB", "TOOLONGLIBNAME", 37).validate())
                .hasMessageContaining("IBMI_DATA_LIBRARY");
    }

    @Test
    void toStringMasksPassword() {
        assertThat(new Jt400Properties("h", "u", "secret", "LIB", null, 37).toString())
                .doesNotContain("secret");
    }
}

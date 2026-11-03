package com.example.clients.repository.jt400;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Jt400PropertiesTest {

    @Test
    void normalizesValidObjectNames() {
        Jt400Properties props = new Jt400Properties("host", "user", "pw", " clientlib ", null, null);
        assertThat(props.library()).isEqualTo("CLIENTLIB");
        assertThat(props.program()).isEqualTo("CLIECRUD");
        assertThat(new Jt400ClientRepository(props).overrideCommand())
                .isEqualTo("OVRDBF FILE(TRCLIE) TOFILE(CLIENTLIB/TRCLIE) OVRSCOPE(*JOB)");
    }

    @Test
    void rejectsLibraryNamesThatCouldAlterSqlOrCl() {
        assertThrows(IllegalArgumentException.class,
                () -> new Jt400Properties("h", "u", "p", "QSYS2.SYSTABLES --", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Jt400Properties("h", "u", "p", "LIB) TOFILE(X", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Jt400Properties("h", "u", "p", "TOOLONGLIBNAME", null, null));
    }

    @Test
    void jt400RepositoryRequiresALibrary() {
        assertThrows(IllegalStateException.class,
                () -> new Jt400ClientRepository(new Jt400Properties("h", "u", "p", "", null, null)));
    }
}

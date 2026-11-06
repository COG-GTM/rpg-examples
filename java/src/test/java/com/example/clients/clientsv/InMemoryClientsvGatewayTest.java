package com.example.clients.clientsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** The emulator must follow the CLIENTSV guard chain exactly. */
class InMemoryClientsvGatewayTest {

    private final InMemoryClientsvGateway gw =
            new InMemoryClientsvGateway("WEBAPP", Clock.fixed(Instant.parse("2026-11-06T19:00:00Z"), ZoneOffset.UTC));

    private ClientsvResponse run(ClientsvOp op, long dni, String name) {
        return gw.execute(ClientsvRequest.of(op, dni, name));
    }

    @Test
    void dniRangeIsExclusive() {
        assertThat(run(ClientsvOp.CREATE, 999_999, "X").code()).isEqualTo("INVDNI");
        assertThat(run(ClientsvOp.CREATE, 1_000_000, "X").success()).isTrue();
        assertThat(run(ClientsvOp.CREATE, 99_999_999, "X").success()).isTrue();
        assertThat(run(ClientsvOp.READ, 0, "").code()).isEqualTo("INVDNI");
    }

    @Test
    void crudLifecycleWithSoftDelete() {
        ClientsvResponse created = run(ClientsvOp.CREATE, 12345678, "ANA");
        assertThat(created.client().createdBy()).isEqualTo("WEBAPP");
        assertThat(run(ClientsvOp.CREATE, 12345678, "DUP").code()).isEqualTo("DUPDNI");

        ClientsvResponse updated = run(ClientsvOp.UPDATE, 12345678, "ANA MARIA");
        assertThat(updated.client().name()).isEqualTo("ANA MARIA");
        assertThat(updated.client().updatedAt()).isNotNull();

        ClientsvResponse deleted = run(ClientsvOp.DELETE, 12345678, "");
        assertThat(deleted.success()).isTrue();
        assertThat(deleted.client().deletedBy()).isEqualTo("WEBAPP");
        assertThat(deleted.client().active()).isFalse();

        // record kept, but R/U/D now fail with DELETED and create still sees it as existing
        assertThat(gw.snapshot()).hasSize(1);
        assertThat(run(ClientsvOp.READ, 12345678, "").code()).isEqualTo("DELETED");
        assertThat(run(ClientsvOp.UPDATE, 12345678, "Y").code()).isEqualTo("DELETED");
        assertThat(run(ClientsvOp.DELETE, 12345678, "").code()).isEqualTo("DELETED");
        assertThat(run(ClientsvOp.CREATE, 12345678, "Z").code()).isEqualTo("DUPDNI");
        assertThat(gw.list(false)).isEmpty();
        assertThat(gw.list(true)).hasSize(1);
    }

    @Test
    void missingClientIsNotFound() {
        assertThat(run(ClientsvOp.READ, 7654321, "").code()).isEqualTo("NOTFND");
        assertThat(run(ClientsvOp.UPDATE, 7654321, "X").code()).isEqualTo("NOTFND");
        assertThat(run(ClientsvOp.DELETE, 7654321, "").code()).isEqualTo("NOTFND");
    }

    @Test
    void unknownOpIsInvop() {
        assertThat(gw.execute(new ClientsvRequest('X', 12345678, "")).code()).isEqualTo("INVOP");
    }
}

package com.example.clients.clientsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises the jt400 gateway's byte path against a fake CLIENTSV (no IBM i needed). */
class Jt400ClientsvGatewayTest {

    @Test
    void encodesRequestAndDecodesServiceProgramOutput() {
        ClientsvCodec codec = new ClientsvCodec();
        InMemoryClientsvGateway fakeRpg = new InMemoryClientsvGateway("QUSER", Clock.systemUTC());
        List<byte[]> calls = new ArrayList<>();
        ServiceProgramInvoker invoker = in -> {
            calls.add(in);
            return codec.encodeResponse(fakeRpg.execute(codec.decodeRequest(in)));
        };
        Jt400ClientsvGateway gw = new Jt400ClientsvGateway(invoker, codec, null);

        ClientsvResponse created = gw.execute(ClientsvRequest.of(ClientsvOp.CREATE, 30111222, "JUAN"));
        assertThat(created.success()).isTrue();
        assertThat(created.client().dni()).isEqualTo(30111222);
        assertThat(created.client().name()).isEqualTo("JUAN");
        assertThat(created.client().createdBy()).isEqualTo("QUSER");
        assertThat(created.client().updatedAt()).isNull();

        ClientsvResponse dup = gw.execute(ClientsvRequest.of(ClientsvOp.CREATE, 30111222, "JUAN"));
        assertThat(dup.success()).isFalse();
        assertThat(dup.code()).isEqualTo("DUPDNI");
        assertThat(dup.message()).isEqualTo("DNI already exists");

        assertThat(calls).hasSize(2).allSatisfy(b -> assertThat(b).hasSize(ClientsvCodec.IN_LENGTH));
    }

    @Test
    void listQueryFiltersSoftDeletedRows() {
        assertThat(Jt400ClientsvGateway.LIST_SQL).contains("FROM TRCLIE");
        assertThat(Jt400ClientsvGateway.ACTIVE_ONLY).isEqualTo(" WHERE CLDEUS = ''");
    }
}

package com.example.clients.clientsv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.clients.domain.Client;
import com.ibm.as400.access.AS400Text;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ClientsvCodecTest {

    private final ClientsvCodec codec = new ClientsvCodec();

    @Test
    void layoutsMatchClientsvH() {
        assertThat(ClientsvCodec.IN_LENGTH).isEqualTo(39);
        assertThat(ClientsvCodec.CLIENT_LENGTH).isEqualTo(146);
        assertThat(ClientsvCodec.OUT_LENGTH).isEqualTo(209);
    }

    @Test
    void requestIsEbcdicOpZonedDniAndPaddedName() {
        byte[] in = codec.encodeRequest(ClientsvRequest.of(ClientsvOp.CREATE, 12345678, "ANA"));
        assertThat(in).hasSize(39);
        assertThat(in[0]).isEqualTo((byte) 0xC3); // 'C' in CCSID 37
        // zoned 12345678: F1 F2 ... F7 F8 (positive sign nibble F)
        assertThat(in[1]).isEqualTo((byte) 0xF1);
        assertThat(in[8]).isEqualTo((byte) 0xF8);
        assertThat(new AS400Text(30, 37).toObject(in, 9)).isEqualTo("ANA" + " ".repeat(27));
        assertThat(codec.decodeRequest(in)).isEqualTo(new ClientsvRequest('C', 12345678, "ANA"));
    }

    @Test
    void responseRoundTripsWithNotSetTimestamps() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 6, 14, 36, 0, 123_456_000);
        Client c = new Client(1234567, "LUIS", created, "QPGMR", null, null, null, null);
        byte[] out = codec.encodeResponse(ClientsvResponse.ok("Client created", c));
        assertThat(out).hasSize(209);
        int clcrts = 1 + 10 + 52 + 8 + 30;
        assertThat(new AS400Text(26, 37).toObject(out, clcrts)).isEqualTo("2026-11-06-14.36.00.123456");
        assertThat(new AS400Text(26, 37).toObject(out, clcrts + 36)).isEqualTo(ClientsvCodec.TS_NONE);

        ClientsvResponse decoded = codec.decodeResponse(out);
        assertThat(decoded.success()).isTrue();
        assertThat(decoded.code()).isEqualTo("OK");
        assertThat(decoded.message()).isEqualTo("Client created");
        assertThat(decoded.client()).isEqualTo(c);
    }

    @Test
    void failureHasNoClient() {
        byte[] out = codec.encodeResponse(new ClientsvResponse(false, "DELETED", "Client marked as deleted", null));
        ClientsvResponse decoded = codec.decodeResponse(out);
        assertThat(decoded.success()).isFalse();
        assertThat(decoded.code()).isEqualTo("DELETED");
        assertThat(decoded.client()).isNull();
    }

    @Test
    void parsesSqlAndIsoTimestamps() {
        assertThat(ClientsvCodec.timestamp("2026-01-02 03:04:05.000006"))
                .isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000));
        assertThat(ClientsvCodec.timestamp("2026-01-02-03.04.05.000006"))
                .isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4, 5, 6_000));
        assertThat(ClientsvCodec.timestamp(ClientsvCodec.TS_NONE)).isNull();
        assertThat(ClientsvCodec.timestamp("  ")).isNull();
    }

    @Test
    void rejectsShortBuffers() {
        assertThatThrownBy(() -> codec.decodeResponse(new byte[10])).isInstanceOf(IllegalArgumentException.class);
    }
}

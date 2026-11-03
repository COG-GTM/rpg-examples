package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CliesrvCodecTest {

    private final CliesrvCodec codec = new CliesrvCodec(37);

    @Test
    void layoutsMatchCopyMember() {
        assertThat(codec.encodeStatus(true, "OK", "x")).hasSize(CliesrvCodec.STATUS_LENGTH);
        Client c = new Client(12_345_678L, "Ana", LocalDateTime.now(), "U", null, "", null, "");
        assertThat(codec.encodeClient(c)).hasSize(CliesrvCodec.CLIENT_LENGTH);
        assertThat(codec.encodeDni(12_345_678L)).hasSize(8);
        assertThat(codec.encodeName("Ana")).hasSize(30);
        assertThat(codec.encodeOp('R')).containsExactly((byte) 0xD9); // 'R' in EBCDIC 37
    }

    @Test
    void decodesRejectedStatus() {
        CliesrvCodec.Status s = codec.decodeStatus(codec.encodeStatus(false, "DELETED", "Client marked as deleted"));
        assertThat(s.success()).isFalse();
        assertThat(s.code()).isEqualTo(ClientStatus.DELETED);
        assertThat(s.message()).isEqualTo("Client marked as deleted");
    }

    @Test
    void decodesClientImageAndLowTimestampsAsNull() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 3, 9, 30, 0, 123_456_000);
        Client in = new Client(12_345_678L, "Ana Maria", created, "QPGMR", null, "", null, "");
        Client out = codec.decodeClient(codec.encodeClient(in));
        assertThat(out).isEqualTo(in);
        assertThat(out.active()).isTrue();
    }

    @Test
    void unknownCodeMapsToIoError() {
        assertThat(ClientStatus.fromRpgCode("??")).isEqualTo(ClientStatus.IO_ERROR);
    }
}

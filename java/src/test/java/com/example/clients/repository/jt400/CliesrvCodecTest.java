package com.example.clients.repository.jt400;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.clients.domain.Client;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CliesrvCodecTest {

    private static final byte EBCDIC_SPACE = 0x40;
    private final CliesrvCodec codec = new CliesrvCodec();

    @Test
    void layoutLengthsMatchContractA() {
        assertThat(CliesrvCodec.STATUS_LENGTH).isEqualTo(63);
        assertThat(CliesrvCodec.CLIENT_LENGTH).isEqualTo(146);
    }

    @Test
    void inputParametersAreEbcdic() {
        assertThat(codec.encodeOp('C')).containsExactly(0xC3);
        assertThat(codec.encodeDni(12345678)).containsExactly(0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8);
        assertThat(codec.encodeDni(1000000)).containsExactly(0xF0, 0xF1, 0xF0, 0xF0, 0xF0, 0xF0, 0xF0, 0xF0);
        byte[] name = codec.encodeName("ANA");
        assertThat(name).hasSize(30);
        assertThat(Arrays.copyOf(name, 4)).containsExactly(0xC1, 0xD5, 0xC1, 0x40);
        assertThat(codec.encodeName("x".repeat(40))).hasSize(30);
    }

    @Test
    void statusByteLayout() {
        byte[] bytes = codec.encodeStatus(new CliesrvStatus(false, "DELETED", "Client marked as deleted"));
        assertThat(bytes).hasSize(63);
        assertThat(bytes[0]).isEqualTo((byte) 0xF0); // success ind '0'
        // code char(10) at offset 1: "DELETED" + 3 blanks
        assertThat(Arrays.copyOfRange(bytes, 1, 11))
                .containsExactly(0xC4, 0xC5, 0xD3, 0xC5, 0xE3, 0xC5, 0xC4, 0x40, 0x40, 0x40);
        // message char(52) at offset 11: 'C' then blank padding up to byte 62
        assertThat(bytes[11]).isEqualTo((byte) 0xC3);
        assertThat(bytes[62]).isEqualTo(EBCDIC_SPACE);

        assertThat(codec.decodeStatus(bytes)).isEqualTo(new CliesrvStatus(false, "DELETED", "Client marked as deleted"));
        byte[] ok = codec.encodeStatus(new CliesrvStatus(true, "OK", "Client read"));
        assertThat(ok[0]).isEqualTo((byte) 0xF1);
        assertThat(codec.decodeStatus(ok)).isEqualTo(new CliesrvStatus(true, "OK", "Client read"));
    }

    @Test
    void clientByteLayout() {
        TrclieRow row = new TrclieRow(12345678, "ANA GARCIA", "2026-11-06-10.15.30.123456", "WEBAPP",
                "0001-01-01-00.00.00.000000", "", "0001-01-01-00.00.00.000000", "");
        byte[] bytes = codec.encodeClient(row);
        assertThat(bytes).hasSize(146);
        // dni zoned(8:0) at 0..7
        assertThat(Arrays.copyOfRange(bytes, 0, 8)).containsExactly(0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8);
        // name char(30) at 8..37
        assertThat(bytes[8]).isEqualTo((byte) 0xC1);
        assertThat(bytes[37]).isEqualTo(EBCDIC_SPACE);
        // createdTs char(26) at 38..63: "2026-..." ; '2' = F2, '-' = 60
        assertThat(Arrays.copyOfRange(bytes, 38, 43)).containsExactly(0xF2, 0xF0, 0xF2, 0xF6, 0x60);
        // createdBy char(10) at 64..73
        assertThat(Arrays.copyOfRange(bytes, 64, 70)).containsExactly(0xE6, 0xC5, 0xC2, 0xC1, 0xD7, 0xD7);
        // updatedTs at 74, updatedBy at 100, deletedTs at 110, deletedBy at 136..145
        assertThat(Arrays.copyOfRange(bytes, 74, 78)).containsExactly(0xF0, 0xF0, 0xF0, 0xF1);
        assertThat(Arrays.copyOfRange(bytes, 100, 110)).containsOnly(EBCDIC_SPACE);
        assertThat(Arrays.copyOfRange(bytes, 110, 114)).containsExactly(0xF0, 0xF0, 0xF0, 0xF1);
        assertThat(Arrays.copyOfRange(bytes, 136, 146)).containsOnly(EBCDIC_SPACE);

        Client c = codec.decodeClient(bytes);
        assertThat(c).isEqualTo(new Client(12345678, "ANA GARCIA",
                LocalDateTime.of(2026, 11, 6, 10, 15, 30, 123_456_000), "WEBAPP", null, null, null, null, true));
    }

    @Test
    void deletedClientDecodesAsInactive() {
        TrclieRow row = new TrclieRow(20000000, "BOB", "2026-01-01-00.00.00.000000", "QPGMR",
                "2026-01-02-00.00.00.000001", "QPGMR", "2026-01-03-12.30.45.000000", "WEBAPP");
        Client c = codec.decodeClient(codec.encodeClient(row));
        assertThat(c.active()).isFalse();
        assertThat(c.deletedBy()).isEqualTo("WEBAPP");
        assertThat(c.deletedAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 12, 30, 45));
        assertThat(c.updatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 0, 0, 0, 1000));
    }

    @Test
    void timestampParsing() {
        assertThat(CliesrvCodec.timestamp("0001-01-01-00.00.00.000000")).isNull();
        assertThat(CliesrvCodec.timestamp("0001-01-01 00:00:00.000000")).isNull();
        assertThat(CliesrvCodec.timestamp("   ")).isNull();
        assertThat(CliesrvCodec.timestamp(null)).isNull();
        assertThat(CliesrvCodec.timestamp("2026-11-06 10:15:30.000001"))
                .isEqualTo(LocalDateTime.of(2026, 11, 6, 10, 15, 30, 1000));
        assertThatThrownBy(() -> CliesrvCodec.timestamp("garbage")).isInstanceOf(IllegalArgumentException.class);
        assertThat(CliesrvCodec.formatTimestamp(null)).isEqualTo("0001-01-01-00.00.00.000000");
    }

    @Test
    void shortBuffersAreRejected() {
        assertThatThrownBy(() -> codec.decodeStatus(new byte[62])).hasMessageContaining("63");
        assertThatThrownBy(() -> codec.decodeClient(new byte[145])).hasMessageContaining("146");
    }

    @Test
    void ccsidIsConfigurable() {
        // CCSID 500 (international EBCDIC) maps '!' to 0x4F, CCSID 37 maps it to 0x5A
        assertThat(new CliesrvCodec(500).encodeOp('!')).containsExactly(0x4F);
        assertThat(new CliesrvCodec(37).encodeOp('!')).containsExactly(0x5A);
    }
}

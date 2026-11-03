package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliesrvCodecTest {

    private static final Charset EBCDIC_37 = Charset.forName("IBM037");
    private final CliesrvCodec codec = new CliesrvCodec(37);

    @Test
    void statusLayoutIs63BytesAndRoundTrips() {
        CliesrvStatus status = new CliesrvStatus(false, "DUPDNI", "DNI already exists");
        byte[] bytes = codec.encodeStatus(status);

        assertThat(bytes).hasSize(CliesrvCodec.STATUS_LENGTH);
        String text = new String(bytes, EBCDIC_37);
        assertThat(text.substring(0, 1)).isEqualTo("0");
        assertThat(text.substring(1, 11)).isEqualTo("DUPDNI    ");
        assertThat(text.substring(11, 63)).isEqualTo(String.format("%-52s", "DNI already exists"));

        CliesrvStatus decoded = codec.decodeStatus(bytes);
        assertThat(decoded).isEqualTo(status);
        assertThat(decoded.errorCode()).isEqualTo(ClientErrorCode.DUPDNI);
    }

    @Test
    void decodesStatusProducedByRpg() {
        byte[] rpg = ebcdic(String.format("1%-10s%-52s", "OK", ""));
        CliesrvStatus status = codec.decodeStatus(rpg);
        assertThat(status.success()).isTrue();
        assertThat(status.errorCode()).isEqualTo(ClientErrorCode.OK);
        assertThat(status.message()).isEmpty();
    }

    @Test
    void unknownStatusCodeMapsToIoerr() {
        assertThat(new CliesrvStatus(false, "WHAT", "").errorCode()).isEqualTo(ClientErrorCode.IOERR);
        assertThat(new CliesrvStatus(false, "INVOP", "").errorCode()).isEqualTo(ClientErrorCode.INVOP);
    }

    @Test
    void clientLayoutIs146BytesAndRoundTrips() {
        Client client = new Client(12345678, "ANA PEREZ",
                LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123_456_000), "QUSER",
                LocalDateTime.of(2026, 11, 4, 8, 0, 0, 1_000), "WEBAPP",
                LocalDateTime.of(2026, 11, 5, 23, 59, 59, 999_999_000), "QSECOFR");
        byte[] bytes = codec.encodeClient(client);

        assertThat(bytes).hasSize(CliesrvCodec.CLIENT_LENGTH);
        // zoned(8:0) 12345678 = F1 F2 F3 F4 F5 F6 F7 F8 (positive sign F in last zone)
        assertThat(Arrays.copyOfRange(bytes, 0, 8)).isEqualTo(ebcdic("12345678"));
        String text = new String(bytes, EBCDIC_37);
        assertThat(text.substring(8, 38)).isEqualTo(String.format("%-30s", "ANA PEREZ"));
        assertThat(text.substring(38, 64)).isEqualTo("2026-11-03-10.15.30.123456");
        assertThat(text.substring(64, 74)).isEqualTo("QUSER     ");
        assertThat(text.substring(74, 100)).isEqualTo("2026-11-04-08.00.00.000001");
        assertThat(text.substring(100, 110)).isEqualTo("WEBAPP    ");
        assertThat(text.substring(110, 136)).isEqualTo("2026-11-05-23.59.59.999999");
        assertThat(text.substring(136, 146)).isEqualTo("QSECOFR   ");

        assertThat(codec.decodeClient(bytes)).isEqualTo(client);
    }

    @Test
    void notSetTimestampsAndBlankUsersDecodeToNull() {
        String image = "12345678"
                + String.format("%-30s", "ANA PEREZ")
                + "2026-11-03-10.15.30.123456" + String.format("%-10s", "QUSER")
                + CliesrvCodec.NULL_TIMESTAMP + " ".repeat(10)
                + CliesrvCodec.NULL_TIMESTAMP + " ".repeat(10);
        assertThat(image).hasSize(146);

        Client client = codec.decodeClient(ebcdic(image));

        assertThat(client.dni()).isEqualTo(12345678);
        assertThat(client.name()).isEqualTo("ANA PEREZ");
        assertThat(client.createdAt()).isEqualTo(LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123_456_000));
        assertThat(client.createdBy()).isEqualTo("QUSER");
        assertThat(client.updatedAt()).isNull();
        assertThat(client.updatedBy()).isNull();
        assertThat(client.deletedAt()).isNull();
        assertThat(client.deletedBy()).isNull();
        assertThat(client.active()).isTrue();

        // and nulls encode back to 0001-01-01 / blanks
        assertThat(codec.encodeClient(client)).isEqualTo(ebcdic(image));
    }

    @Test
    void blankTimestampIsAlsoNull() {
        assertThat(CliesrvCodec.parseTimestamp(" ".repeat(26))).isNull();
        assertThat(CliesrvCodec.parseTimestamp(null)).isNull();
        assertThatThrownBy(() -> CliesrvCodec.parseTimestamp("2026-13-01-00.00.00.000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputParametersAreEncodedAsEbcdicAndZoned() {
        assertThat(codec.encodeOp('R')).isEqualTo(ebcdic("R"));
        assertThat(codec.encodeDni(1000000)).isEqualTo(ebcdic("01000000"));
        assertThat(codec.encodeName("ANA")).isEqualTo(ebcdic(String.format("%-30s", "ANA")));
        assertThat(codec.encodeName("")).isEqualTo(CliesrvCodec.blanks(30));
        assertThatThrownBy(() -> codec.encodeName("X".repeat(31))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsShortBuffers() {
        assertThatThrownBy(() -> codec.decodeStatus(new byte[10])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decodeClient(new byte[145])).isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] ebcdic(String s) {
        return s.getBytes(EBCDIC_37);
    }
}

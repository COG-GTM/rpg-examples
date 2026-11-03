package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Jt400ClientRepositoryTest {

    private final CliesrvCodec codec = new CliesrvCodec(37);
    private final Client image = new Client(12345678, "ANA PEREZ",
            LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123_456_000), "QUSER", null, null, null, null);

    @Test
    void successfulCallDecodesClientImageAndPassesParameters() {
        FakeGateway gateway = new FakeGateway(new CliesrvStatus(true, "OK", ""), image);
        Jt400ClientRepository repo = new Jt400ClientRepository(gateway, codec);

        assertThat(repo.create(12345678, "ANA PEREZ")).isEqualTo(image);
        assertThat(repo.findActive(12345678)).isEqualTo(image);
        assertThat(repo.update(12345678, "ANA")).isEqualTo(image);
        assertThat(repo.softDelete(12345678)).isEqualTo(image);
        assertThat(gateway.calls).containsExactly("C:12345678:ANA PEREZ", "R:12345678:", "U:12345678:ANA",
                "D:12345678:");
    }

    @Test
    void rejectedStatusBecomesTypedException() {
        for (String code : List.of("DUPDNI", "NOTFND", "DELETED", "INVDNI", "IOERR", "INVOP")) {
            FakeGateway gateway = new FakeGateway(new CliesrvStatus(false, code, "msg " + code), null);
            Jt400ClientRepository repo = new Jt400ClientRepository(gateway, codec);
            assertThatThrownBy(() -> repo.findActive(12345678))
                    .isInstanceOfSatisfying(ClientException.class, e -> {
                        assertThat(e.code()).isEqualTo(ClientErrorCode.valueOf(code));
                        assertThat(e.getMessage()).isEqualTo("msg " + code);
                    });
        }
    }

    @Test
    void inconsistentStatusIsIoerr() {
        Jt400ClientRepository repo = new Jt400ClientRepository(
                new FakeGateway(new CliesrvStatus(false, "OK", ""), null), codec);
        assertThatThrownBy(() -> repo.findActive(12345678))
                .isInstanceOfSatisfying(ClientException.class,
                        e -> assertThat(e.code()).isEqualTo(ClientErrorCode.IOERR));
    }

    @Test
    void outOfRangeDniIsRejectedWithoutCallingIbmi() {
        FakeGateway gateway = new FakeGateway(new CliesrvStatus(true, "OK", ""), image);
        Jt400ClientRepository repo = new Jt400ClientRepository(gateway, codec);
        assertThatThrownBy(() -> repo.findActive(100000000))
                .isInstanceOfSatisfying(ClientException.class,
                        e -> assertThat(e.code()).isEqualTo(ClientErrorCode.INVDNI));
        assertThat(gateway.calls).isEmpty();
    }

    @Test
    void listDelegatesToQueryAndMapsJdbcRows() {
        Client row = CliesrvCodec.toClient(10000000, "A".repeat(3) + " ".repeat(27),
                "2026-11-03-10.15.30.123456", "QUSER     ",
                CliesrvCodec.NULL_TIMESTAMP, "          ",
                CliesrvCodec.NULL_TIMESTAMP, "          ");
        assertThat(row.name()).isEqualTo("AAA");
        assertThat(row.updatedAt()).isNull();
        assertThat(row.updatedBy()).isNull();
        assertThat(row.active()).isTrue();

        FakeGateway gateway = new FakeGateway(null, null);
        gateway.rows = List.of(row);
        assertThat(new Jt400ClientRepository(gateway, codec).findAllActive()).containsExactly(row);
    }

    @Test
    void listSqlMatchesContract() {
        assertThat(Jt400CliecrudGateway.LIST_SQL)
                .contains("FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI");
    }

    private final class FakeGateway implements CliecrudGateway {
        final List<String> calls = new ArrayList<>();
        final CliesrvStatus status;
        final Client client;
        List<Client> rows = List.of();

        FakeGateway(CliesrvStatus status, Client client) {
            this.status = status;
            this.client = client;
        }

        @Override
        public CliecrudResponse call(char op, long dni, String name) {
            calls.add(op + ":" + dni + ":" + name);
            byte[] clientBytes = client == null
                    ? CliesrvCodec.blanks(CliesrvCodec.CLIENT_LENGTH)
                    : codec.encodeClient(client);
            return new CliecrudResponse(codec.encodeStatus(status), clientBytes);
        }

        @Override
        public List<Client> selectActive() {
            return rows;
        }
    }
}

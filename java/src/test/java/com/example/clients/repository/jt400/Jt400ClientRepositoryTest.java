package com.example.clients.repository.jt400;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Jt400ClientRepositoryTest {

    private static final String SENTINEL = "0001-01-01-00.00.00.000000";
    private static final TrclieRow ANA = new TrclieRow(12345678, "ANA", "2026-11-06-10.15.30.123456", "WEBAPP",
            SENTINEL, "", SENTINEL, "");

    private final CliesrvCodec codec = new CliesrvCodec();
    private final FakeCliecrudGateway gateway = new FakeCliecrudGateway(codec);
    private final Jt400ClientRepository repo = new Jt400ClientRepository(gateway, codec);

    @Test
    void createCallsCliecrudWithOpCAndDecodesClientImage() {
        gateway.reply(new CliesrvStatus(true, "OK", "Client created"), ANA);
        Client c = repo.create(12345678, "ANA");
        assertThat(gateway.calls).containsExactly(new FakeCliecrudGateway.Call('C', 12345678, "ANA"));
        assertThat(c).isEqualTo(new Client(12345678, "ANA", LocalDateTime.of(2026, 11, 6, 10, 15, 30, 123_456_000),
                "WEBAPP", null, null, null, null, true));
    }

    @Test
    void readUpdateDeleteUseTheirOpCodes() {
        gateway.reply(new CliesrvStatus(true, "OK", ""), ANA);
        repo.read(12345678);
        repo.update(12345678, "ANA MARIA");
        repo.delete(12345678);
        assertThat(gateway.calls).extracting(FakeCliecrudGateway.Call::op).containsExactly('R', 'U', 'D');
        assertThat(gateway.calls.get(1).name()).isEqualTo("ANA MARIA");
    }

    @ParameterizedTest
    @CsvSource({
        "INVDNI,DNI not valid,INVDNI",
        "DUPDNI,DNI already exists,DUPDNI",
        "NOTFND,DNI not exists,NOTFND",
        "DELETED,Client marked as deleted,DELETED",
        "IOERR,'I/O error on TRCLIE, status 1218',IOERR",
        "INVOP,OP. Error,INVOP",
        "BOGUS,something odd,IOERR",
    })
    void failedStatusBecomesClientExceptionWithLegacyMessage(String code, String message, ClientErrorCode expected) {
        gateway.reply(new CliesrvStatus(false, code, message), null);
        assertThatThrownBy(() -> repo.read(12345678))
                .isInstanceOf(ClientException.class)
                .hasMessage(message)
                .extracting(e -> ((ClientException) e).code()).isEqualTo(expected);
    }

    @Test
    void outOfRangeDniNeverReachesTheProgram() {
        assertThatThrownBy(() -> repo.read(100_000_000)).isInstanceOf(ClientException.class)
                .extracting(e -> ((ClientException) e).code()).isEqualTo(ClientErrorCode.INVDNI);
        assertThat(gateway.calls).isEmpty();
    }

    @Test
    void listMapsJdbcRows() {
        gateway.rows.add(ANA);
        gateway.rows.add(new TrclieRow(23456789, "BOB                           ", "2026-01-01-00.00.00.000000",
                "QPGMR     ", "2026-02-01-08.00.00.500000", "WEBAPP    ", SENTINEL, "          "));
        assertThat(repo.findAllActive()).containsExactly(
                new Client(12345678, "ANA", LocalDateTime.of(2026, 11, 6, 10, 15, 30, 123_456_000), "WEBAPP",
                        null, null, null, null, true),
                new Client(23456789, "BOB", LocalDateTime.of(2026, 1, 1, 0, 0), "QPGMR",
                        LocalDateTime.of(2026, 2, 1, 8, 0, 0, 500_000_000), "WEBAPP", null, null, true));
    }

    @Test
    void listQueryMatchesContract() {
        assertThat(Jt400CliecrudGateway.LIST_SQL).isEqualTo("SELECT CLDNI, CLNAME, CHAR(CLCRTS), CLCRUS, "
                + "CHAR(CLUPTS), CLUPUS, CHAR(CLDETS), CLDEUS FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI");
    }
}

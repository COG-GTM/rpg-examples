package com.example.clients.repository.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryClientRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-06T10:15:30.123456789Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 10, 15, 30, 123_456_000);

    private InMemoryClientRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryClientRepository("WEBAPP", CLOCK);
    }

    private static void assertCode(ThrowingCallable call, ClientErrorCode code, String message) {
        assertThatThrownBy(call).isInstanceOf(ClientException.class)
                .hasMessage(message)
                .extracting(e -> ((ClientException) e).code()).isEqualTo(code);
    }

    @Test
    void createStampsCreationAuditOnlyWithMicroseconds() {
        Client c = repo.create(12345678, "ANA GARCIA   ");
        assertThat(c).isEqualTo(new Client(12345678, "ANA GARCIA", NOW, "WEBAPP", null, null, null, null, true));
    }

    @Test
    void auditUserIsTrimmedAndCutTo10Chars() {
        Client c = new InMemoryClientRepository(" VERYLONGUSERNAME ", CLOCK).create(12345678, "ANA");
        assertThat(c.createdBy()).isEqualTo("VERYLONGUS");
    }

    @Test
    void dniRangeIsCheckedFirst() {
        for (long dni : new long[] {999_999, 100_000_000, 0, -5}) {
            assertCode(() -> repo.create(dni, "X"), ClientErrorCode.INVDNI, "DNI not valid");
            assertCode(() -> repo.read(dni), ClientErrorCode.INVDNI, "DNI not valid");
            assertCode(() -> repo.update(dni, "X"), ClientErrorCode.INVDNI, "DNI not valid");
            assertCode(() -> repo.delete(dni), ClientErrorCode.INVDNI, "DNI not valid");
        }
        assertThat(repo.create(1_000_000, "MIN").dni()).isEqualTo(1_000_000);
        assertThat(repo.create(99_999_999, "MAX").dni()).isEqualTo(99_999_999);
    }

    @Test
    void duplicateDniIsRejected() {
        repo.create(12345678, "ANA");
        assertCode(() -> repo.create(12345678, "OTHER"), ClientErrorCode.DUPDNI, "DNI already exists");
    }

    @Test
    void missingDniIsNotFound() {
        assertCode(() -> repo.read(12345678), ClientErrorCode.NOTFND, "DNI not exists");
        assertCode(() -> repo.update(12345678, "X"), ClientErrorCode.NOTFND, "DNI not exists");
        assertCode(() -> repo.delete(12345678), ClientErrorCode.NOTFND, "DNI not exists");
    }

    @Test
    void updateKeepsCreationAuditAndStampsUpdate() {
        repo.create(12345678, "ANA");
        Client u = repo.update(12345678, "ANA MARIA");
        assertThat(u).isEqualTo(new Client(12345678, "ANA MARIA", NOW, "WEBAPP", NOW, "WEBAPP", null, null, true));
        assertThat(repo.read(12345678)).isEqualTo(u);
    }

    @Test
    void softDeletedClientIsGoneForReadUpdateDeleteAndCannotBeRecreated() {
        repo.create(12345678, "ANA");
        repo.delete(12345678);
        assertCode(() -> repo.read(12345678), ClientErrorCode.DELETED, "Client marked as deleted");
        assertCode(() -> repo.update(12345678, "X"), ClientErrorCode.DELETED, "Client marked as deleted");
        assertCode(() -> repo.delete(12345678), ClientErrorCode.DELETED, "Client marked as deleted");
        assertCode(() -> repo.create(12345678, "ANA"), ClientErrorCode.DUPDNI, "DNI already exists");
    }

    @Test
    void listReturnsOnlyActiveClientsOrderedByDni() {
        repo.create(30000000, "C");
        repo.create(10000000, "A");
        repo.create(20000000, "B");
        repo.delete(20000000);
        assertThat(repo.findAllActive()).extracting(Client::dni).containsExactly(10000000L, 30000000L);
    }
}

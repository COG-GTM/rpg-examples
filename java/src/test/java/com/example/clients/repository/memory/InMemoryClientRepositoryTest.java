package com.example.clients.repository.memory;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryClientRepositoryTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123_456_789);

    private MutableClock clock;
    private InMemoryClientRepository repo;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(T0.toInstant(ZoneOffset.UTC));
        repo = new InMemoryClientRepository("WEBAPP", clock);
    }

    @Test
    void createStampsCreationAuditAndTruncatesToMicros() {
        Client c = repo.create(12345678, "ANA PEREZ   ");
        assertThat(c.dni()).isEqualTo(12345678);
        assertThat(c.name()).isEqualTo("ANA PEREZ");
        assertThat(c.createdAt()).isEqualTo(LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123_456_000));
        assertThat(c.createdBy()).isEqualTo("WEBAPP");
        assertThat(c.updatedAt()).isNull();
        assertThat(c.updatedBy()).isNull();
        assertThat(c.deletedAt()).isNull();
        assertThat(c.deletedBy()).isNull();
        assertThat(c.active()).isTrue();
        assertThat(repo.findActive(12345678)).isEqualTo(c);
    }

    @Test
    void duplicateCreateIsDupdni() {
        repo.create(12345678, "ANA");
        assertCode(() -> repo.create(12345678, "OTHER"), ClientErrorCode.DUPDNI, "DNI already exists");
    }

    @Test
    void createOfSoftDeletedDniIsStillDupdni() {
        repo.create(12345678, "ANA");
        repo.softDelete(12345678);
        assertCode(() -> repo.create(12345678, "ANA"), ClientErrorCode.DUPDNI, "DNI already exists");
    }

    @Test
    void invalidDniWinsOverEverything() {
        assertCode(() -> repo.create(999999, "X"), ClientErrorCode.INVDNI, "DNI not valid");
        assertCode(() -> repo.findActive(100000000), ClientErrorCode.INVDNI, "DNI not valid");
        assertCode(() -> repo.update(0, "X"), ClientErrorCode.INVDNI, "DNI not valid");
        assertCode(() -> repo.softDelete(-5), ClientErrorCode.INVDNI, "DNI not valid");
    }

    @Test
    void unknownDniIsNotfnd() {
        assertCode(() -> repo.findActive(12345678), ClientErrorCode.NOTFND, "DNI not exists");
        assertCode(() -> repo.update(12345678, "X"), ClientErrorCode.NOTFND, "DNI not exists");
        assertCode(() -> repo.softDelete(12345678), ClientErrorCode.NOTFND, "DNI not exists");
    }

    @Test
    void updateStampsUpdateAuditAndKeepsCreation() {
        Client created = repo.create(12345678, "ANA");
        clock.advanceSeconds(60);
        Client updated = repo.update(12345678, "ANA MARIA");
        assertThat(updated.name()).isEqualTo("ANA MARIA");
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
        assertThat(updated.createdBy()).isEqualTo("WEBAPP");
        assertThat(updated.updatedAt()).isEqualTo(created.createdAt().plusSeconds(60));
        assertThat(updated.updatedBy()).isEqualTo("WEBAPP");
        assertThat(updated.active()).isTrue();
    }

    @Test
    void softDeleteStampsDeletionAndKeepsRecord() {
        repo.create(12345678, "ANA");
        clock.advanceSeconds(5);
        Client deleted = repo.softDelete(12345678);
        assertThat(deleted.name()).isEqualTo("ANA");
        assertThat(deleted.deletedAt()).isEqualTo(T0.plusSeconds(5).withNano(123_456_000));
        assertThat(deleted.deletedBy()).isEqualTo("WEBAPP");
        assertThat(deleted.active()).isFalse();
    }

    @Test
    void readUpdateDeleteOfDeletedClientIsDeleted() {
        repo.create(12345678, "ANA");
        repo.softDelete(12345678);
        assertCode(() -> repo.findActive(12345678), ClientErrorCode.DELETED, "Client marked as deleted");
        assertCode(() -> repo.update(12345678, "X"), ClientErrorCode.DELETED, "Client marked as deleted");
        assertCode(() -> repo.softDelete(12345678), ClientErrorCode.DELETED, "Client marked as deleted");
    }

    @Test
    void listReturnsActiveOnlyOrderedByDni() {
        repo.create(30000000, "C");
        repo.create(10000000, "A");
        repo.create(20000000, "B");
        repo.softDelete(20000000);
        assertThat(repo.findAllActive()).extracting(Client::dni).containsExactly(10000000L, 30000000L);
    }

    @Test
    void auditUserIsConfigurable() {
        InMemoryClientRepository custom = new InMemoryClientRepository("QUSER", clock);
        assertThat(custom.create(12345678, "ANA").createdBy()).isEqualTo("QUSER");
        assertThatThrownBy(() -> new InMemoryClientRepository("WAY_TOO_LONG_USER", clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertCode(Executable call, ClientErrorCode code, String message) {
        assertThatThrownBy(call::execute)
                .isInstanceOfSatisfying(ClientException.class, e -> {
                    assertThat(e.code()).isEqualTo(code);
                    assertThat(e.getMessage()).isEqualTo(message);
                });
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

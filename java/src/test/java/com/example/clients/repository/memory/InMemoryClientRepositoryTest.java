package com.example.clients.repository.memory;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientOperationException;
import com.example.clients.domain.ClientStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryClientRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T10:15:30Z"), ZoneOffset.UTC);
    private final InMemoryClientRepository repo = new InMemoryClientRepository("devin_audit_user", CLOCK);

    @Test
    void dniRangeIsExclusiveOnBothEnds() {
        expect(ClientStatus.INVALID_DNI, "DNI not valid", () -> repo.create(999_999L, "x"));
        expect(ClientStatus.INVALID_DNI, "DNI not valid", () -> repo.create(100_000_000L, "x"));
        repo.create(1_000_000L, "Min");
        repo.create(99_999_999L, "Max");
        assertThat(repo.findAllActive()).extracting(Client::dni).containsExactly(1_000_000L, 99_999_999L);
    }

    @Test
    void createStampsAuditFieldsAndRejectsDuplicates() {
        repo.create(12_345_678L, "Ana");
        Client c = repo.read(12_345_678L);
        assertThat(c.createdAt()).isEqualTo(LocalDateTime.of(2026, 11, 3, 10, 15, 30));
        assertThat(c.createdBy()).isEqualTo("DEVIN_AUDI");
        assertThat(c.updatedAt()).isNull();
        assertThat(c.active()).isTrue();
        expect(ClientStatus.DUPLICATE, "DNI already exists", () -> repo.create(12_345_678L, "Again"));
    }

    @Test
    void readUpdateDeleteRequireExistingActiveClient() {
        expect(ClientStatus.NOT_FOUND, "DNI not exists", () -> repo.read(12_345_678L));
        expect(ClientStatus.NOT_FOUND, "DNI not exists", () -> repo.update(12_345_678L, "x"));
        expect(ClientStatus.NOT_FOUND, "DNI not exists", () -> repo.delete(12_345_678L));
        expect(ClientStatus.INVALID_DNI, "DNI not valid", () -> repo.read(5L));
    }

    @Test
    void updateStampsUpdateAuditFields() {
        repo.create(12_345_678L, "Ana");
        repo.update(12_345_678L, "Ana Maria");
        Client c = repo.read(12_345_678L);
        assertThat(c.name()).isEqualTo("Ana Maria");
        assertThat(c.updatedBy()).isEqualTo("DEVIN_AUDI");
        assertThat(c.updatedAt()).isNotNull();
    }

    @Test
    void deleteIsSoftAndBlocksFurtherOperationsAndRecreation() {
        repo.create(12_345_678L, "Ana");
        repo.delete(12_345_678L);
        assertThat(repo.findAllActive()).isEmpty();
        expect(ClientStatus.DELETED, "Client marked as deleted", () -> repo.read(12_345_678L));
        expect(ClientStatus.DELETED, "Client marked as deleted", () -> repo.update(12_345_678L, "x"));
        expect(ClientStatus.DELETED, "Client marked as deleted", () -> repo.delete(12_345_678L));
        expect(ClientStatus.DUPLICATE, "DNI already exists", () -> repo.create(12_345_678L, "Ana"));
    }

    @Test
    void nameIsTruncatedToClnameLength() {
        repo.create(12_345_678L, "x".repeat(40));
        assertThat(repo.read(12_345_678L).name()).hasSize(30);
    }

    private static void expect(ClientStatus status, String message, Executable call) {
        ClientOperationException e = assertThrows(ClientOperationException.class, call);
        assertThat(e.status()).isEqualTo(status);
        assertThat(e.getMessage()).isEqualTo(message);
    }
}

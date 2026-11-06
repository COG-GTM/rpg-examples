package com.example.clients.repository.memory;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.Dni;
import com.example.clients.repository.ClientRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** In-process TRCLIE with the CLIESRV rules, so the API runs without an IBM i. */
public class InMemoryClientRepository implements ClientRepository {

    static final int NAME_LENGTH = 30;
    static final int USER_LENGTH = 10;

    private final Map<Long, Client> rows = new TreeMap<>();
    private final String auditUser;
    private final Clock clock;

    public InMemoryClientRepository(String auditUser) {
        this(auditUser, Clock.systemDefaultZone());
    }

    public InMemoryClientRepository(String auditUser, Clock clock) {
        this.auditUser = normalizeUser(auditUser);
        this.clock = clock;
    }

    @Override
    public synchronized List<Client> findAllActive() {
        return rows.values().stream().filter(Client::active).toList();
    }

    @Override
    public synchronized Client read(long dni) {
        return activeRow(dni);
    }

    @Override
    public synchronized Client create(long dni, String name) {
        requireValidDni(dni);
        if (rows.containsKey(dni)) {
            throw new ClientException(ClientErrorCode.DUPDNI);
        }
        Client created = new Client(dni, normalizeName(name), now(), auditUser, null, null, null, null, true);
        rows.put(dni, created);
        return created;
    }

    @Override
    public synchronized Client update(long dni, String name) {
        Client row = activeRow(dni);
        Client updated = new Client(dni, normalizeName(name), row.createdAt(), row.createdBy(),
                now(), auditUser, null, null, true);
        rows.put(dni, updated);
        return updated;
    }

    @Override
    public synchronized void delete(long dni) {
        Client row = activeRow(dni);
        rows.put(dni, new Client(dni, row.name(), row.createdAt(), row.createdBy(),
                row.updatedAt(), row.updatedBy(), now(), auditUser, false));
    }

    private Client activeRow(long dni) {
        requireValidDni(dni);
        Client row = rows.get(dni);
        if (row == null) {
            throw new ClientException(ClientErrorCode.NOTFND);
        }
        if (!row.active()) {
            throw new ClientException(ClientErrorCode.DELETED);
        }
        return row;
    }

    private static void requireValidDni(long dni) {
        if (!Dni.isValid(dni)) {
            throw new ClientException(ClientErrorCode.INVDNI);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static String normalizeName(String name) {
        String n = name == null ? "" : name.stripTrailing();
        return n.length() > NAME_LENGTH ? n.substring(0, NAME_LENGTH).stripTrailing() : n;
    }

    private static String normalizeUser(String user) {
        String u = user == null ? "" : user.trim();
        if (u.isEmpty()) {
            throw new IllegalArgumentException("clients.audit-user must not be blank");
        }
        return u.length() > USER_LENGTH ? u.substring(0, USER_LENGTH) : u;
    }
}

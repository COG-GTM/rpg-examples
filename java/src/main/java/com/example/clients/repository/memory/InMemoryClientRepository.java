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

/**
 * Local stand-in for TRCLIE that applies the same CLIESRV rules, so the API and the
 * Angular app can run without an IBM i. Data lives only for the lifetime of the JVM.
 */
public class InMemoryClientRepository implements ClientRepository {

    private final Map<Long, Client> table = new TreeMap<>();
    private final String auditUser;
    private final Clock clock;

    public InMemoryClientRepository(String auditUser, Clock clock) {
        if (auditUser == null || auditUser.isBlank() || auditUser.trim().length() > 10) {
            throw new IllegalArgumentException("auditUser must be 1-10 characters (CLCRUS is char(10))");
        }
        this.auditUser = auditUser.trim();
        this.clock = clock;
    }

    @Override
    public synchronized Client create(long dni, String name) {
        requireValid(dni);
        if (table.containsKey(dni)) {
            throw new ClientException(ClientErrorCode.DUPDNI);
        }
        Client client = new Client(dni, normalize(name), now(), auditUser, null, null, null, null);
        table.put(dni, client);
        return client;
    }

    @Override
    public synchronized Client findActive(long dni) {
        return requireActive(dni);
    }

    @Override
    public synchronized Client update(long dni, String name) {
        Client current = requireActive(dni);
        Client updated = new Client(dni, normalize(name), current.createdAt(), current.createdBy(),
                now(), auditUser, null, null);
        table.put(dni, updated);
        return updated;
    }

    @Override
    public synchronized Client softDelete(long dni) {
        Client current = requireActive(dni);
        Client deleted = new Client(dni, current.name(), current.createdAt(), current.createdBy(),
                current.updatedAt(), current.updatedBy(), now(), auditUser);
        table.put(dni, deleted);
        return deleted;
    }

    @Override
    public synchronized List<Client> findAllActive() {
        return table.values().stream().filter(Client::active).toList();
    }

    private Client requireActive(long dni) {
        requireValid(dni);
        Client client = table.get(dni);
        if (client == null) {
            throw new ClientException(ClientErrorCode.NOTFND);
        }
        if (!client.active()) {
            throw new ClientException(ClientErrorCode.DELETED);
        }
        return client;
    }

    private static void requireValid(long dni) {
        if (!Dni.isValid(dni)) {
            throw new ClientException(ClientErrorCode.INVDNI);
        }
    }

    /** CLNAME is char(30): trailing blanks are padding and are not returned. */
    private static String normalize(String name) {
        return name == null ? "" : name.stripTrailing();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}

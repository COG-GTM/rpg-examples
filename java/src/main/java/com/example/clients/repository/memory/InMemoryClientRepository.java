package com.example.clients.repository.memory;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientOperationException;
import com.example.clients.domain.ClientRules;
import com.example.clients.domain.ClientStatus;
import com.example.clients.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Local/dev implementation (no IBM i). Re-implements CLIESRV's rules line for line so the API and
 * the Angular app behave exactly as they will against TRCLIE.
 */
@Repository
@ConditionalOnProperty(name = "clients.repository", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryClientRepository implements ClientRepository {

    private final Map<Long, Client> rows = new TreeMap<>();
    private final String currentUser;
    private final Clock clock;

    @Autowired
    public InMemoryClientRepository(@Value("${clients.audit-user:WEBAPI}") String auditUser) {
        this(auditUser, Clock.systemDefaultZone());
    }

    InMemoryClientRepository(String auditUser, Clock clock) {
        String user = auditUser == null ? "" : auditUser.trim().toUpperCase(Locale.ROOT);
        if (user.isEmpty()) {
            // a blank CLDEUS means "active", so a blank audit user would make deletes no-ops
            throw new IllegalArgumentException("clients.audit-user must not be blank");
        }
        this.currentUser = user.length() > ClientRules.USER_MAX_LENGTH
                ? user.substring(0, ClientRules.USER_MAX_LENGTH) : user;
        this.clock = clock;
    }

    @Override
    public synchronized List<Client> findAllActive() {
        return rows.values().stream().filter(Client::active).toList();
    }

    @Override
    public synchronized Client read(long dni) {
        checkActive(dni);
        return rows.get(dni);
    }

    @Override
    public synchronized void create(long dni, String name) {
        if (!ClientRules.dniIsValid(dni)) {
            throw new ClientOperationException(ClientStatus.INVALID_DNI, "DNI not valid");
        }
        if (rows.containsKey(dni)) {
            throw new ClientOperationException(ClientStatus.DUPLICATE, "DNI already exists");
        }
        rows.put(dni, new Client(dni, fit(name), now(), currentUser, null, "", null, ""));
    }

    @Override
    public synchronized void update(long dni, String name) {
        Client c = checkActive(dni);
        rows.put(dni, new Client(dni, fit(name), c.createdAt(), c.createdBy(),
                now(), currentUser, c.deletedAt(), c.deletedBy()));
    }

    @Override
    public synchronized void delete(long dni) {
        Client c = checkActive(dni);
        rows.put(dni, new Client(dni, c.name(), c.createdAt(), c.createdBy(),
                c.updatedAt(), c.updatedBy(), now(), currentUser));
    }

    private Client checkActive(long dni) {
        if (!ClientRules.dniIsValid(dni)) {
            throw new ClientOperationException(ClientStatus.INVALID_DNI, "DNI not valid");
        }
        Client c = rows.get(dni);
        if (c == null) {
            throw new ClientOperationException(ClientStatus.NOT_FOUND, "DNI not exists");
        }
        if (!c.active()) {
            throw new ClientOperationException(ClientStatus.DELETED, "Client marked as deleted");
        }
        return c;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** CLNAME is CHAR(30): blank padded / truncated on write, trailing blanks dropped on read. */
    private static String fit(String name) {
        String n = name == null ? "" : name.stripTrailing();
        return n.length() > ClientRules.NAME_MAX_LENGTH ? n.substring(0, ClientRules.NAME_MAX_LENGTH) : n;
    }
}

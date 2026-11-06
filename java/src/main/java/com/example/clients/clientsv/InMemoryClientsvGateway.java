package com.example.clients.clientsv;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientsvCode;
import com.example.clients.domain.Dni;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Local stand-in for CLIENTSV: same guard chain, result codes and soft delete, backed by a map.
 * Used when no IBM i is configured ({@code clients.backend=memory}).
 */
public class InMemoryClientsvGateway implements ClientsvGateway {

    private final TreeMap<Long, Client> trclie = new TreeMap<>();
    private final String auditUser;
    private final Clock clock;

    public InMemoryClientsvGateway(String auditUser, Clock clock) {
        this.auditUser = ClientsvCodec.user(auditUser.length() > ClientsvCodec.USER_LENGTH
                ? auditUser.substring(0, ClientsvCodec.USER_LENGTH) : auditUser);
        this.clock = clock;
    }

    @Override
    public synchronized ClientsvResponse execute(ClientsvRequest request) {
        return switch (request.op()) {
            case 'C' -> create(request.dni(), request.name());
            case 'R' -> read(request.dni());
            case 'U' -> update(request.dni(), request.name());
            case 'D' -> delete(request.dni());
            default -> ClientsvResponse.fail(ClientsvCode.INVOP);
        };
    }

    @Override
    public synchronized List<Client> list(boolean includeDeleted) {
        return trclie.values().stream().filter(c -> includeDeleted || c.active()).toList();
    }

    /** Inserts a record image as-is (demo seed data / tests). */
    public synchronized void put(Client client) {
        trclie.put(client.dni(), client);
    }

    private ClientsvResponse create(long dni, String name) {
        if (!Dni.isValid(dni)) {
            return ClientsvResponse.fail(ClientsvCode.INVDNI);
        }
        if (trclie.containsKey(dni)) {
            return ClientsvResponse.fail(ClientsvCode.DUPDNI);
        }
        Client c = new Client(dni, fit(name), now(), auditUser, null, null, null, null);
        trclie.put(dni, c);
        return ClientsvResponse.ok("Client created", c);
    }

    private ClientsvResponse read(long dni) {
        ClientsvResponse guard = guard(dni);
        return guard != null ? guard : ClientsvResponse.ok("Client read", trclie.get(dni));
    }

    private ClientsvResponse update(long dni, String name) {
        ClientsvResponse guard = guard(dni);
        if (guard != null) {
            return guard;
        }
        Client o = trclie.get(dni);
        Client c = new Client(dni, fit(name), o.createdAt(), o.createdBy(), now(), auditUser,
                o.deletedAt(), o.deletedBy());
        trclie.put(dni, c);
        return ClientsvResponse.ok("Client updated", c);
    }

    private ClientsvResponse delete(long dni) {
        ClientsvResponse guard = guard(dni);
        if (guard != null) {
            return guard;
        }
        Client o = trclie.get(dni);
        Client c = new Client(dni, o.name(), o.createdAt(), o.createdBy(), o.updatedAt(), o.updatedBy(),
                now(), auditUser);
        trclie.put(dni, c);
        return ClientsvResponse.ok("Client deleted", c);
    }

    /** valid DNI -> exists -> active; null when all checks pass. */
    private ClientsvResponse guard(long dni) {
        if (!Dni.isValid(dni)) {
            return ClientsvResponse.fail(ClientsvCode.INVDNI);
        }
        Client c = trclie.get(dni);
        if (c == null) {
            return ClientsvResponse.fail(ClientsvCode.NOTFND);
        }
        if (!c.active()) {
            return ClientsvResponse.fail(ClientsvCode.DELETED);
        }
        return null;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static String fit(String name) {
        String n = name == null ? "" : name.stripTrailing();
        return n.length() > ClientsvCodec.NAME_LENGTH ? n.substring(0, ClientsvCodec.NAME_LENGTH) : n;
    }

    /** For tests: a copy of the stored records. */
    synchronized List<Client> snapshot() {
        return new ArrayList<>(trclie.values());
    }
}

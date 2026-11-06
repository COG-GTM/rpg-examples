package com.example.clients.repository;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientException;
import java.util.List;

/**
 * Persistence seam for TRCLIE clients. Every implementation applies the CLIESRV rules in the same
 * order: INVDNI, then DUPDNI (create) or NOTFND (read/update/delete), then DELETED. Rule violations
 * are reported as {@link ClientException}.
 *
 * <p>Implementations: {@code InMemoryClientRepository} (default), {@code Jt400ClientRepository}
 * ({@code CLIENTS_REPOSITORY=jt400}) and a future {@code JpaClientRepository}
 * ({@code CLIENTS_REPOSITORY=jpa}, see {@code com.example.clients.repository.jpa}).
 */
public interface ClientRepository {

    /** Active clients (CLDEUS blank) ordered by DNI. */
    List<Client> findAllActive();

    Client read(long dni);

    Client create(long dni, String name);

    Client update(long dni, String name);

    /** Soft delete: stamps CLDETS/CLDEUS. */
    void delete(long dni);
}

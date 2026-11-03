package com.example.clients.repository;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientException;

import java.util.List;

/**
 * Port over the client module. Every implementation applies the CLIESRV rules in the
 * legacy order: INVDNI, then DUPDNI (create) / NOTFND (read, update, delete), then
 * DELETED (read, update, delete). Rejections are reported as {@link ClientException}
 * carrying the CLIESRV code.
 *
 * <p>Implementations: {@code InMemoryClientRepository} (default),
 * {@code Jt400ClientRepository} (CLIECRUD over jt400). A future
 * {@code JpaClientRepository} can map an entity directly onto TRCLIE.
 */
public interface ClientRepository {

    /** createClient: writes a new active record and returns its image. */
    Client create(long dni, String name) throws ClientException;

    /** readClient: returns the record only if it exists and is active. */
    Client findActive(long dni) throws ClientException;

    /** updateClient: changes the name and stamps CLUPTS/CLUPUS. */
    Client update(long dni, String name) throws ClientException;

    /** deleteClient: stamps CLDETS/CLDEUS (soft delete) and returns the image. */
    Client softDelete(long dni) throws ClientException;

    /** All active clients (CLDEUS blank) ordered by DNI. */
    List<Client> findAllActive() throws ClientException;
}

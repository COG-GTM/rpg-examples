package com.example.clients.repository;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientOperationException;

import java.util.List;

/**
 * Port over the TRCLIE client store. Contracts mirror the CLIESRV service program; every
 * implementation (jt400/RPG today, JPA on TRCLIE later) must enforce the same invariants and
 * signal rejections with {@link ClientOperationException} carrying the matching
 * {@link com.example.clients.domain.ClientStatus}.
 *
 * <ul>
 *   <li>Validation order is always: DNI valid, then exists, then active.</li>
 *   <li>Soft-deleted clients still "exist": they block re-creation of the same DNI.</li>
 * </ul>
 */
public interface ClientRepository {

    /** Active clients only (CLDEUS blank), ordered by DNI. */
    List<Client> findAllActive();

    /** CLIESRV readClient: INVALID_DNI, NOT_FOUND or DELETED when rejected. */
    Client read(long dni);

    /** CLIESRV createClient: INVALID_DNI or DUPLICATE when rejected. Stamps CLCRTS/CLCRUS. */
    void create(long dni, String name);

    /** CLIESRV updateClient: INVALID_DNI, NOT_FOUND or DELETED when rejected. Stamps CLUPTS/CLUPUS. */
    void update(long dni, String name);

    /** CLIESRV deleteClient (soft delete): INVALID_DNI, NOT_FOUND or DELETED. Stamps CLDETS/CLDEUS. */
    void delete(long dni);
}

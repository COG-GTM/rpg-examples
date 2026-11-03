package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientException;

import java.util.List;

/** Transport to the IBM i: CLIECRUD program call plus the TRCLIE list query. */
public interface CliecrudGateway {

    /**
     * Calls CLIECRUD in structured (5-parameter) mode.
     *
     * @throws ClientException with {@code IOERR} when the system cannot be reached or the call fails
     */
    CliecrudResponse call(char op, long dni, String name);

    /** {@code SELECT ... FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI}. */
    List<Client> selectActive();
}

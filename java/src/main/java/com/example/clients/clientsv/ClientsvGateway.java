package com.example.clients.clientsv;

import com.example.clients.domain.Client;
import java.util.List;

/** Transport to the CLIENTSV service program (or an emulation of it). */
public interface ClientsvGateway {

    /** Calls {@code clientsvExecute(in: out)}. Never throws for business failures. */
    ClientsvResponse execute(ClientsvRequest request);

    /** Read-only listing of TRCLIE ordered by DNI (CLIENTSV has no list procedure). */
    List<Client> list(boolean includeDeleted);
}

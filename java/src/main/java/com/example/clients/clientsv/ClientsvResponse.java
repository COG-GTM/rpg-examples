package com.example.clients.clientsv;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientsvCode;

/** {@code clientsv_out_t}: success ind | code char(10) | message char(52) | client client_t. */
public record ClientsvResponse(boolean success, String code, String message, Client client) {

    public ClientsvCode codeEnum() {
        return ClientsvCode.fromWire(code);
    }

    public static ClientsvResponse ok(String message, Client client) {
        return new ClientsvResponse(true, ClientsvCode.OK.name(), message, client);
    }

    public static ClientsvResponse fail(ClientsvCode code) {
        return new ClientsvResponse(false, code.name(), code.legacyMessage(), null);
    }
}

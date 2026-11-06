package com.example.clients.service;

import com.example.clients.clientsv.ClientsvGateway;
import com.example.clients.clientsv.ClientsvOp;
import com.example.clients.clientsv.ClientsvRequest;
import com.example.clients.clientsv.ClientsvResponse;
import com.example.clients.domain.Client;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.ClientsvCode;
import java.util.List;
import org.springframework.stereotype.Service;

/** Maps C/R/U/D onto {@code clientsvExecute}; any unsuccessful result becomes a {@link ClientException}. */
@Service
public class ClientService {

    private final ClientsvGateway gateway;

    public ClientService(ClientsvGateway gateway) {
        this.gateway = gateway;
    }

    public Client create(long dni, String name) {
        return call(ClientsvOp.CREATE, dni, name);
    }

    public Client read(long dni) {
        return call(ClientsvOp.READ, dni, "");
    }

    public Client update(long dni, String name) {
        return call(ClientsvOp.UPDATE, dni, name);
    }

    public Client delete(long dni) {
        return call(ClientsvOp.DELETE, dni, "");
    }

    public List<Client> list(boolean includeDeleted) {
        return gateway.list(includeDeleted);
    }

    private Client call(ClientsvOp op, long dni, String name) {
        ClientsvResponse res = gateway.execute(ClientsvRequest.of(op, dni, name));
        if (!res.success()) {
            ClientsvCode code = res.codeEnum();
            String message = res.message() == null || res.message().isBlank() ? code.legacyMessage() : res.message();
            throw new ClientException(code, message);
        }
        if (res.client() == null) {
            throw new ClientException(ClientsvCode.IOERR, "CLIENTSV returned no client record");
        }
        return res.client();
    }
}

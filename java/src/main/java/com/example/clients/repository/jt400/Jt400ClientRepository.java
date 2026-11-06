package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.Dni;
import com.example.clients.repository.ClientRepository;
import java.util.List;

/**
 * {@link ClientRepository} backed by CLIECRUD/CLIESRV on IBM i. CRUD goes through the program so
 * CLIESRV stays the single owner of the rules; the list is a plain JDBC select on TRCLIE.
 */
public class Jt400ClientRepository implements ClientRepository {

    static final char OP_CREATE = 'C';
    static final char OP_READ = 'R';
    static final char OP_UPDATE = 'U';
    static final char OP_DELETE = 'D';

    private final CliecrudGateway gateway;
    private final CliesrvCodec codec;

    public Jt400ClientRepository(CliecrudGateway gateway, CliesrvCodec codec) {
        this.gateway = gateway;
        this.codec = codec;
    }

    @Override
    public List<Client> findAllActive() {
        return gateway.selectActive().stream().map(CliesrvCodec::toClient).toList();
    }

    @Override
    public Client read(long dni) {
        return invoke(OP_READ, dni, "");
    }

    @Override
    public Client create(long dni, String name) {
        return invoke(OP_CREATE, dni, name);
    }

    @Override
    public Client update(long dni, String name) {
        return invoke(OP_UPDATE, dni, name);
    }

    @Override
    public void delete(long dni) {
        invoke(OP_DELETE, dni, "");
    }

    private Client invoke(char op, long dni, String name) {
        // zoned(8:0) cannot carry larger values; CLIESRV would answer INVDNI for them anyway.
        if (!Dni.isValid(dni)) {
            throw new ClientException(ClientErrorCode.INVDNI);
        }
        CliecrudReply reply = gateway.call(op, dni, name);
        CliesrvStatus status = codec.decodeStatus(reply.status());
        if (!status.success()) {
            throw new ClientException(ClientErrorCode.fromStatusCode(status.code()), status.message());
        }
        return codec.decodeClient(reply.client());
    }
}

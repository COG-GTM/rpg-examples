package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.Dni;
import com.example.clients.repository.ClientRepository;

import java.util.List;

/**
 * {@link ClientRepository} backed by the RPG service program: CRUD goes through
 * {@code CLIECRUD} (bound to {@code CLIESRV}) so the RPG rules stay the source of truth;
 * the list is a read-only SQL query on TRCLIE.
 */
public class Jt400ClientRepository implements ClientRepository {

    private static final String NO_NAME = "";

    private final CliecrudGateway gateway;
    private final CliesrvCodec codec;

    public Jt400ClientRepository(CliecrudGateway gateway, CliesrvCodec codec) {
        this.gateway = gateway;
        this.codec = codec;
    }

    @Override
    public Client create(long dni, String name) {
        return invoke('C', dni, name);
    }

    @Override
    public Client findActive(long dni) {
        return invoke('R', dni, NO_NAME);
    }

    @Override
    public Client update(long dni, String name) {
        return invoke('U', dni, name);
    }

    @Override
    public Client softDelete(long dni) {
        return invoke('D', dni, NO_NAME);
    }

    @Override
    public List<Client> findAllActive() {
        return gateway.selectActive();
    }

    private Client invoke(char op, long dni, String name) {
        // zoned(8:0) cannot carry an out-of-range DNI, and INVDNI is checked first by CLIESRV anyway.
        if (!Dni.isValid(dni)) {
            throw new ClientException(ClientErrorCode.INVDNI);
        }
        CliecrudResponse response = gateway.call(op, dni, name);
        CliesrvStatus status = codec.decodeStatus(response.status());
        ClientErrorCode code = status.errorCode();
        if (!status.success() || code != ClientErrorCode.OK) {
            throw new ClientException(code == ClientErrorCode.OK ? ClientErrorCode.IOERR : code, status.message());
        }
        return codec.decodeClient(response.client());
    }
}

package com.example.clients.repository.jt400;

import java.util.ArrayList;
import java.util.List;

/** Replays canned CLIECRUD replies encoded with the real codec and records the calls made. */
class FakeCliecrudGateway implements CliecrudGateway {

    record Call(char op, long dni, String name) {
    }

    final List<Call> calls = new ArrayList<>();
    final List<TrclieRow> rows = new ArrayList<>();
    private final CliesrvCodec codec;
    private CliesrvStatus nextStatus = new CliesrvStatus(true, "OK", "");
    private TrclieRow nextClient;

    FakeCliecrudGateway(CliesrvCodec codec) {
        this.codec = codec;
    }

    void reply(CliesrvStatus status, TrclieRow client) {
        nextStatus = status;
        nextClient = client;
    }

    @Override
    public CliecrudReply call(char op, long dni, String name) {
        calls.add(new Call(op, dni, name));
        byte[] client = nextClient == null ? new byte[CliesrvCodec.CLIENT_LENGTH] : codec.encodeClient(nextClient);
        return new CliecrudReply(codec.encodeStatus(nextStatus), client);
    }

    @Override
    public List<TrclieRow> selectActive() {
        return rows;
    }
}
